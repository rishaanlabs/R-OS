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
an upgrade can destroy data.

Usage: verify_room_migration.py <schema-dir> <migration.kt> <from-version> <to-version>
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

# Tables that already hold user data before this migration runs. The migration may not create,
# drop, alter or delete from any of them.
PRE_EXISTING_TABLES = {
    "inbox_items",
    "projects",
    "tasks",
    "waiting_items",
    "notes",
    "daily_reviews",
}

DESTRUCTIVE = re.compile(
    r"\b(DROP\s+TABLE|DROP\s+INDEX|DELETE\s+FROM|TRUNCATE|ALTER\s+TABLE)\b", re.IGNORECASE
)

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

    # ---- 1. The migration must not touch anything that already holds data.
    for raw in statements:
        statement = normalise(raw)
        if DESTRUCTIVE.search(statement):
            problems.append(f"destructive statement in migration: {statement[:120]}")
        for _, table in enumerate(TABLE_POSITIONS.findall(statement)):
            if table in PRE_EXISTING_TABLES:
                problems.append(
                    f"migration operates on the pre-existing table '{table}': {statement[:120]}"
                )

    new_schema = load_schema(schema_dir, to_version)["database"]
    new_tables = {e["tableName"] for e in new_schema["entities"]}

    # The v1 schema was never committed, so fall back to the known table list when it is absent.
    old_schema = load_schema(schema_dir, from_version, required=False)
    if old_schema is not None:
        old_tables = {e["tableName"] for e in old_schema["database"]["entities"]}
        removed = old_tables - new_tables
        if removed:
            problems.append(
                f"tables present in v{from_version} but gone in v{to_version}: {sorted(removed)}"
            )
    else:
        old_tables = set(PRE_EXISTING_TABLES)
        print(f"(no exported v{from_version} schema; using the known pre-existing table list)")

    missing = PRE_EXISTING_TABLES - new_tables
    if missing:
        problems.append(f"tables that hold user data are no longer in the schema: {sorted(missing)}")

    added = new_tables - old_tables
    print(f"v{from_version} tables: {len(old_tables)}   v{to_version} tables: {len(new_tables)}")
    print(f"added by this migration: {sorted(added)}")

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

    if problems:
        print("\nMigration check FAILED:\n")
        for problem in problems:
            print(f"  - {problem}")
        print(
            "\nShipping this would crash the app on launch for anyone upgrading, or lose data.\n"
        )
        return 1

    print(f"\nMigration {from_version} -> {to_version} matches the exported schema exactly.")
    print("No pre-existing table is created, altered, dropped or deleted from.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
