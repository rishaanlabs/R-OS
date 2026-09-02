#!/usr/bin/env python3
"""Mutation tests for verify_room_migration.py.

The migration checker is the only thing standing between a wrong migration and an app that dies on
launch against the user's real database. A checker that silently passes everything looks exactly
like a checker that works, so each case below breaks the migration in one specific way and asserts
that the check notices.

The cases are the ways a migration actually goes wrong: a forgotten index, a relaxed NOT NULL, a
column whose default drifts from the entity, a destructive statement, and — the class the checker
could not see at all before — anything to do with ALTER TABLE.

Run: python3 scripts/test_verify_room_migration.py
"""

from __future__ import annotations

import json
import pathlib
import subprocess
import sys
import tempfile

HERE = pathlib.Path(__file__).resolve().parent
CHECKER = HERE / "verify_room_migration.py"

ACCOUNTS_SQL = (
    "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` ("
    "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `openingBalanceMinor` INTEGER NOT NULL, "
    "PRIMARY KEY(`id`))"
)
ACCOUNTS_INDEX = (
    "CREATE INDEX IF NOT EXISTS `index_finance_accounts_name` ON `${TABLE_NAME}` (`name`)"
)


def entity(table: str, create_sql: str, fields: list[dict], indices: list[dict] | None = None) -> dict:
    return {
        "tableName": table,
        "createSql": create_sql,
        "fields": fields,
        "indices": indices or [],
        "primaryKey": {"columnNames": ["id"], "autoGenerate": False},
        "foreignKeys": [],
    }


def field(name: str, affinity: str = "TEXT", not_null: bool = True, default=None) -> dict:
    f = {"fieldPath": name, "columnName": name, "affinity": affinity, "notNull": not_null}
    if default is not None:
        f["defaultValue"] = default
    return f


TASKS = entity("tasks", "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` TEXT NOT NULL, PRIMARY KEY(`id`))", [field("id")])

ACCOUNTS = entity(
    "finance_accounts",
    ACCOUNTS_SQL,
    [field("id"), field("name"), field("openingBalanceMinor", "INTEGER")],
    [{"name": "index_finance_accounts_name", "createSql": ACCOUNTS_INDEX}],
)


def schema(version: int, entities: list[dict]) -> dict:
    return {"formatVersion": 1, "database": {"version": version, "entities": entities, "views": []}}


def run(case: str, schemas: dict[int, dict], migration: str, frm: int, to: int) -> tuple[int, str]:
    """Run the checker over a throwaway schema directory and migration file."""
    with tempfile.TemporaryDirectory() as tmp:
        root = pathlib.Path(tmp)
        schema_dir = root / "schemas" / "debug"
        schema_dir.mkdir(parents=True)
        for version, content in schemas.items():
            (schema_dir / f"{version}.json").write_text(json.dumps(content))

        migration_file = root / "Migration.kt"
        migration_file.write_text(migration)

        result = subprocess.run(
            [sys.executable, str(CHECKER), str(root / "schemas"), str(migration_file), str(frm), str(to)],
            capture_output=True,
            text=True,
        )
        return result.returncode, result.stdout + result.stderr


def kt(*statements: str) -> str:
    """Wrap SQL the way the real migration file writes it."""
    body = "\n".join(f'        db.execSQL("""{s}""".trimIndent())' for s in statements)
    return "val MIGRATION = object : Migration(1, 2) {\n    override fun migrate(db: Any) {\n" + body + "\n    }\n}\n"


CASES: list[tuple[str, bool, dict, str, int, int]] = []


def case(name: str, should_pass: bool, schemas: dict, migration: str, frm: int = 1, to: int = 2):
    CASES.append((name, should_pass, schemas, migration, frm, to))


# ---------------------------------------------------------------------------------------------
# Creating a new table. The checks that already existed, kept so the rewrite cannot lose them.
# ---------------------------------------------------------------------------------------------

good_create = ACCOUNTS_SQL.replace("${TABLE_NAME}", "finance_accounts")
good_index = ACCOUNTS_INDEX.replace("${TABLE_NAME}", "finance_accounts")

case(
    "a correct new table passes",
    True,
    {1: schema(1, [TASKS]), 2: schema(2, [TASKS, ACCOUNTS])},
    kt(good_create, good_index),
)

case(
    "a forgotten index fails",
    False,
    {1: schema(1, [TASKS]), 2: schema(2, [TASKS, ACCOUNTS])},
    kt(good_create),
)

case(
    "a relaxed NOT NULL fails",
    False,
    {1: schema(1, [TASKS]), 2: schema(2, [TASKS, ACCOUNTS])},
    kt(good_create.replace("`name` TEXT NOT NULL", "`name` TEXT"), good_index),
)

case(
    "a smuggled DROP TABLE fails",
    False,
    {1: schema(1, [TASKS]), 2: schema(2, [TASKS, ACCOUNTS])},
    kt(good_create, good_index, "DROP TABLE `tasks`"),
)

case(
    "recreating a table that already holds data fails",
    False,
    {1: schema(1, [TASKS]), 2: schema(2, [TASKS, ACCOUNTS])},
    kt(good_create, good_index, "CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, PRIMARY KEY(`id`))"),
)

case(
    "deleting from a table that already holds data fails",
    False,
    {1: schema(1, [TASKS]), 2: schema(2, [TASKS, ACCOUNTS])},
    kt(good_create, good_index, "DELETE FROM `tasks`"),
)

case(
    "losing a table between versions fails",
    False,
    {1: schema(1, [TASKS, ACCOUNTS]), 2: schema(2, [TASKS])},
    kt("CREATE INDEX IF NOT EXISTS `noop` ON `tasks` (`id`)"),
)

# ---------------------------------------------------------------------------------------------
# Adding a column. None of this could be expressed before, let alone checked.
# ---------------------------------------------------------------------------------------------

REVIEWED = field("reviewed", "INTEGER", not_null=True, default="1")
ACCOUNTS_WITH_FLAG = entity(
    "finance_accounts",
    ACCOUNTS_SQL,
    [field("id"), field("name"), field("openingBalanceMinor", "INTEGER"), REVIEWED],
    [{"name": "index_finance_accounts_name", "createSql": ACCOUNTS_INDEX}],
)
V2 = schema(2, [TASKS, ACCOUNTS])
V3 = schema(3, [TASKS, ACCOUNTS_WITH_FLAG])

case(
    "a correct ADD COLUMN passes",
    True,
    {2: V2, 3: V3},
    kt("ALTER TABLE `finance_accounts` ADD COLUMN `reviewed` INTEGER NOT NULL DEFAULT 1"),
    2,
    3,
)

case(
    "ADD COLUMN with the wrong default fails",
    False,
    {2: V2, 3: V3},
    kt("ALTER TABLE `finance_accounts` ADD COLUMN `reviewed` INTEGER NOT NULL DEFAULT 0"),
    2,
    3,
)

case(
    "ADD COLUMN with the wrong affinity fails",
    False,
    {2: V2, 3: V3},
    kt("ALTER TABLE `finance_accounts` ADD COLUMN `reviewed` TEXT NOT NULL DEFAULT 1"),
    2,
    3,
)

case(
    "ADD COLUMN that forgets NOT NULL fails",
    False,
    {2: V2, 3: V3},
    kt("ALTER TABLE `finance_accounts` ADD COLUMN `reviewed` INTEGER DEFAULT 1"),
    2,
    3,
)

case(
    "a column the entity declares but the migration never adds fails",
    False,
    {2: V2, 3: V3},
    kt("CREATE INDEX IF NOT EXISTS `noop` ON `tasks` (`id`)"),
    2,
    3,
)

case(
    "ADD COLUMN for something no entity declares fails",
    False,
    {2: V2, 3: schema(3, [TASKS, ACCOUNTS])},
    kt("ALTER TABLE `finance_accounts` ADD COLUMN `scratch` TEXT NOT NULL DEFAULT ''"),
    2,
    3,
)

case(
    "ALTER TABLE RENAME fails",
    False,
    {2: V2, 3: V3},
    kt(
        "ALTER TABLE `finance_accounts` ADD COLUMN `reviewed` INTEGER NOT NULL DEFAULT 1",
        "ALTER TABLE `finance_accounts` RENAME TO `old_accounts`",
    ),
    2,
    3,
)

case(
    "ALTER TABLE DROP COLUMN fails",
    False,
    {2: V2, 3: V3},
    kt(
        "ALTER TABLE `finance_accounts` ADD COLUMN `reviewed` INTEGER NOT NULL DEFAULT 1",
        "ALTER TABLE `finance_accounts` DROP COLUMN `name`",
    ),
    2,
    3,
)

# ---------------------------------------------------------------------------------------------
# The failure that started this: no committed from-version schema past v1.
# ---------------------------------------------------------------------------------------------

case(
    "a missing from-version schema fails loudly instead of guessing",
    False,
    {3: V3},
    kt("ALTER TABLE `finance_accounts` ADD COLUMN `reviewed` INTEGER NOT NULL DEFAULT 1"),
    2,
    3,
)


def main() -> int:
    failures = []
    for name, should_pass, schemas, migration, frm, to in CASES:
        code, output = run(name, schemas, migration, frm, to)
        passed = code == 0
        if passed != should_pass:
            failures.append((name, should_pass, code, output))
            print(f"  FAIL  {name}")
        else:
            print(f"  ok    {name}")

    print()
    if failures:
        for name, should_pass, code, output in failures:
            print(f"--- {name}: expected {'pass' if should_pass else 'failure'}, exit {code}")
            print(output)
        print(f"{len(failures)} of {len(CASES)} mutation tests did not behave as expected.")
        return 1

    print(f"All {len(CASES)} mutation tests behaved as expected.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
