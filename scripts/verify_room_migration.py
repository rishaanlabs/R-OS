#!/usr/bin/env python3
"""Check the hand-written Room migration against the schema Room actually expects.

Room validates the database against the entity definitions the first time it opens after an
upgrade. If a migration builds a table even slightly differently from what the entities declare —
a missing index, a nullable column that should be NOT NULL, a foreign key with the wrong delete
rule — Room throws IllegalStateException and the app dies on launch, on a database that holds the
only copy of the user's data.

That check normally only happens on a real device. This script brings it forward to build time by
comparing the migration's SQL with the createSql Room generates into its exported schema JSON.

It also refuses to let the migration touch any table that existed before, which is the other way
an upgrade can destroy data. "Touch" means create, drop, rewrite or delete from — adding a column
is allowed, because ALTER TABLE ... ADD COLUMN is the one form of ALTER that cannot destroy data:
SQLite appends the column and backfills the declared default without rewriting a single row.

An added column is checked against the exported schema too. That is the case Room's own launch-time
validation is strictest about and the one this script used to ignore entirely: a column whose
affinity, nullability or default disagrees with the entity by a single character throws on the
first open after the upgrade.

Usage: verify_room_migration.py <schema-dir> <migration.kt> <from-version> <to-version>
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

# The v1 table list. Only used when there is no exported from-version schema to derive the real
# set from, which now means v1 alone — every later version exports its schema and commits it.
V1_TABLES = {
    "inbox_items",
    "projects",
    "tasks",
    "waiting_items",
    "notes",
    "daily_reviews",
}

# Statements that can destroy data outright, wherever they appear.
DESTRUCTIVE = re.compile(
    r"\b(DROP\s+TABLE|DROP\s+INDEX|DELETE\s+FROM|TRUNCATE)\b", re.IGNORECASE
)

# The only permitted form of ALTER: appending a column. Anything else — RENAME, DROP COLUMN — either
# rewrites the table or removes data, so it is rejected below.
ADD_COLUMN = re.compile(
    r"^\s*ALTER\s+TABLE\s+`?(?P<table>[A-Za-z_][A-Za-z0-9_]*)`?\s+"
    r"ADD\s+(?:COLUMN\s+)?(?P<definition>.+?)\s*$",
    re.IGNORECASE | re.DOTALL,
)

ANY_ALTER = re.compile(r"\bALTER\s+TABLE\b", re.IGNORECASE)

# Where a table name can appear in a statement. Matching only these positions avoids a false
# alarm when a column happens to share a name with a table.
TABLE_POSITIONS = re.compile(
    r"(?:CREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?|REFERENCES|INSERT\s+INTO|UPDATE|DELETE\s+FROM"
    r"|ALTER\s+TABLE|DROP\s+TABLE(?:\s+IF\s+EXISTS)?|\bON)\s+`?([A-Za-z_][A-Za-z0-9_]*)`?",
    re.IGNORECASE,
)


TOKEN = re.compile(r"`[^`]*`|[(),;]|[A-Za-z_][A-Za-z0-9_]*|\S")


def normalise(sql: str) -> str:
    """Tidy a statement for display in an error message."""
    return re.sub(r"\s+", " ", sql).strip().rstrip(";")


def tokens(sql: str) -> tuple[str, ...]:
    """Reduce a statement to its token stream.

    Comparing tokens rather than text is what makes this check meaningful. Room's generated
    createSql is spaced idiosyncratically — a space before the comma separating foreign keys, none
    before the closing parenthesis — and matching that byte for byte would mean rejecting a
    perfectly correct migration. Every token here is either a quoted identifier, a keyword or a
    punctuation mark that delimits itself, so two statements with the same token stream parse to
    exactly the same thing, while any real difference (a dropped NOT NULL, a changed delete rule,
    a missing column) changes the tokens.
    """
    return tuple(t.upper() if not t.startswith("`") else t for t in TOKEN.findall(sql.rstrip().rstrip(";")))


def column_definition(field: dict) -> str:
    """The column definition Room's schema implies, in the form an ADD COLUMN would write it."""
    parts = [f"`{field['columnName']}`", field["affinity"]]
    if field.get("notNull"):
        parts.append("NOT NULL")
    if field.get("defaultValue") is not None:
        parts.append(f"DEFAULT {field['defaultValue']}")
    return " ".join(parts)


def fields_by_table(schema: dict) -> dict[str, dict[str, dict]]:
    """Every entity's columns, keyed by table then column name."""
    return {
        entity["tableName"]: {f["columnName"]: f for f in entity["fields"]}
        for entity in schema["entities"]
    }


def extract_statements(migration_src: str) -> list[str]:
    """Pull every SQL string the migration hands to execSQL."""
    statements: list[str] = []

    # Triple-quoted blocks, which is how the CREATE TABLE statements are written.
    for match in re.finditer(r'execSQL\(\s*"""(.*?)"""', migration_src, re.DOTALL):
        statements.append(match.group(1))

    # Single-line strings, which is how the CREATE INDEX statements are written.
    for match in re.finditer(r'execSQL\(\s*"((?:[^"\\]|\\.)*)"\s*\)', migration_src):
        statements.append(match.group(1))

    return [s for s in statements if s.strip()]


def load_schema(schema_dir: pathlib.Path, version: int, required: bool = True) -> dict | None:
    candidates = sorted(schema_dir.rglob(f"{version}.json"))
    if not candidates:
        if not required:
            return None
        raise SystemExit(
            f"No exported schema {version}.json under {schema_dir}.\n"
            "Room writes it during the build; check that exportSchema is on and the Room "
            "Gradle plugin's schemaDirectory is set."
        )
    # Every variant exports the same schema, so the first is representative.
    return json.loads(candidates[0].read_text())


def main() -> int:
    if len(sys.argv) != 5:
        print(__doc__)
        return 2

    schema_dir = pathlib.Path(sys.argv[1])
    migration_file = pathlib.Path(sys.argv[2])
    from_version = int(sys.argv[3])
    to_version = int(sys.argv[4])

    migration_src = migration_file.read_text()
    statements = extract_statements(migration_src)
    token_index = {tokens(s): s for s in statements}
    if not statements:
        raise SystemExit(f"Found no execSQL statements in {migration_file}")

    problems: list[str] = []

    new_schema = load_schema(schema_dir, to_version)["database"]
    new_tables = {e["tableName"] for e in new_schema["entities"]}

    # Which tables already hold data is derived from the exported from-version schema rather than a
    # literal, so the set grows with the app. v1 is the only version that never exported one.
    old_schema = load_schema(schema_dir, from_version, required=from_version > 1)
    if old_schema is not None:
        protected = {e["tableName"] for e in old_schema["database"]["entities"]}
    else:
        protected = set(V1_TABLES)
        print(f"(no exported v{from_version} schema; using the known v1 table list)")

    old_tables = set(protected)
    added = new_tables - old_tables
    added_columns: list[tuple[str, str]] = []

    # ---- 1. The migration may add, but never rewrite or remove.
    for raw in statements:
        statement = normalise(raw)

        if DESTRUCTIVE.search(statement):
            problems.append(f"destructive statement in migration: {statement[:120]}")
            continue

        add = ADD_COLUMN.match(statement)
        if add:
            added_columns.append((add.group("table"), add.group("definition")))
            continue

        if ANY_ALTER.search(statement):
            problems.append(
                "only ALTER TABLE ... ADD COLUMN is allowed; anything else rewrites the table: "
                f"{statement[:120]}"
            )
            continue

        for table in TABLE_POSITIONS.findall(statement):
            if table in protected:
                problems.append(
                    f"migration operates on the pre-existing table '{table}': {statement[:120]}"
                )

    removed = old_tables - new_tables
    if removed:
        problems.append(
            f"tables present in v{from_version} but gone in v{to_version}: {sorted(removed)}"
        )

    print(f"v{from_version} tables: {len(old_tables)}   v{to_version} tables: {len(new_tables)}")
    print(f"added by this migration: {sorted(added)}")
    if added_columns:
        print(f"columns added: {sorted(f'{t}.{d.split()[0].strip(chr(96))}' for t, d in added_columns)}")

    # ---- 2. Every added table must be created exactly as Room expects it.
    for entity in new_schema["entities"]:
        table = entity["tableName"]
        if table not in added:
            continue

        expected = entity["createSql"].replace("${TABLE_NAME}", table)
        if tokens(expected) not in token_index:
            close = [
                normalise(s) for s in statements
                if f"`{table}`" in s and s.strip().upper().startswith("CREATE TABLE")
            ]
            problems.append(
                f"\n  table '{table}' is not created the way Room expects."
                f"\n    Room expects:  {normalise(expected)}"
                f"\n    migration has: {close[0] if close else '(no CREATE TABLE for this table)'}"
            )

        for index in entity.get("indices", []):
            expected_index = index["createSql"].replace("${TABLE_NAME}", table)
            if tokens(expected_index) not in token_index:
                problems.append(
                    f"\n  index '{index['name']}' on '{table}' is missing or differs."
                    f"\n    Room expects: {normalise(expected_index)}"
                )

    # ---- 3. Every added column must match the column Room expects.
    #
    # Room compares affinity, nullability and default at launch and throws if any of them differs,
    # on the database holding the user's only copy. Nothing checked this before: the table pass
    # above only looks at tables in `added`, so a column bolted onto an existing table went
    # entirely unverified.
    schema_fields = fields_by_table(new_schema)
    for table, definition in added_columns:
        if table not in schema_fields:
            problems.append(
                f"migration adds a column to '{table}', which is not in the v{to_version} schema"
            )
            continue

        name = definition.split()[0].strip("`")
        field = schema_fields[table].get(name)
        if field is None:
            problems.append(
                f"migration adds column '{table}.{name}', which no entity declares"
            )
            continue

        expected = column_definition(field)
        if tokens(expected) != tokens(definition):
            problems.append(
                f"\n  column '{table}.{name}' is not added the way Room expects."
                f"\n    Room expects:  {expected}"
                f"\n    migration has: {normalise(definition)}"
            )

    # A column an entity declares but the migration never adds fails the same way, and is the
    # easier mistake to make: the entity changes and the migration is forgotten.
    for table in sorted(set(schema_fields) & old_tables):
        if old_schema is None:
            break
        old_columns = {
            f["columnName"]
            for entity in old_schema["database"]["entities"]
            if entity["tableName"] == table
            for f in entity["fields"]
        }
        declared_new = set(schema_fields[table]) - old_columns
        migrated = {d.split()[0].strip("`") for t, d in added_columns if t == table}
        forgotten = declared_new - migrated
        if forgotten:
            problems.append(
                f"'{table}' gained {sorted(forgotten)} in v{to_version} but the migration never "
                "adds them"
            )

    if problems:
        print("\nMigration check FAILED:\n")
        for problem in problems:
            print(f"  - {problem}")
        print(
            "\nShipping this would crash the app on launch for anyone upgrading, or lose data.\n"
        )
        return 1

    print(f"\nMigration {from_version} -> {to_version} matches the exported schema exactly.")
    print("No pre-existing table is created, rewritten, dropped or deleted from.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
