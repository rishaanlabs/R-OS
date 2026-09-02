# Room migration 2 → 3 — scope

Three things the R-OS Mobile design needs that the schema cannot express: scheduled payments,
whether a transaction has been reviewed, and a record of what happened to a thing. They are scoped
together because the migration is the expensive, risky part and the features are not — three
migrations to reach the same schema is three upgrade tests on a database holding real money.

**Status: not started.** The V0.1.2 gate is closed (1 → 2 proved on a real phone, lived with for a
fortnight), so writing this one is now allowed. It should not begin until the tooling changes in
§1 are done, for the reason given there.

---

## 1. The tooling blocks this before a line of SQL is written

The migration checker built for 1 → 2 assumes "additive" means "creates new tables and nothing
else". Two of the three changes here break that assumption, and one breaks the check outright.
None of this is visible from the entity definitions — it was found by reading
`scripts/verify_room_migration.py` and `.gitignore`.

### 1a. The exported schema is not committed, so 2 → 3 cannot be checked at all

`.gitignore` excludes `app/schemas/`. Room only exports the *current* version, so the moment the
database becomes version 3 the build produces `3.json` and no `2.json`.

The checker handles a missing from-version schema by falling back to `PRE_EXISTING_TABLES` — the
six V0.1.0 tables. It would therefore compute "tables added by this migration" as *the seven
finance tables plus the new ones*, and demand a `CREATE TABLE` for `finance_accounts` and friends
inside the 2 → 3 migration. CI fails, and the failure message points at the wrong problem.

**Fix:** stop ignoring `app/schemas/` and commit `2.json` before the version is bumped. This is
what Room's own guidance says to do with exported schemas, and it is the only way any migration
after the first can be verified. Committing it also makes future schema changes visible in review
as a diff, which is worth having on its own.

### 1b. `ALTER TABLE` is rejected globally, so no column can ever be added

```python
DESTRUCTIVE = re.compile(r"\b(DROP\s+TABLE|DROP\s+INDEX|DELETE\s+FROM|TRUNCATE|ALTER\s+TABLE)\b", ...)
```

That check runs against every statement, not only statements touching pre-existing tables. Adding
`reviewed` to `finance_transactions` needs `ALTER TABLE … ADD COLUMN`, which the checker refuses.

The rule was right for 1 → 2, where nothing needed altering and `ALTER TABLE` could only mean a
mistake. It is too strong now: `ADD COLUMN` is the one form of `ALTER TABLE` that cannot destroy
data — SQLite appends a column and backfills the default, it does not rewrite rows.

**Fix:** allow `ALTER TABLE <table> ADD COLUMN` specifically, and keep rejecting every other form
(`RENAME`, `DROP COLUMN`, anything else). Narrow the pattern rather than deleting the check.

### 1c. Added columns are not verified against what Room expects

The checker only compares `createSql` for tables in the *added* set. A column added to an existing
table is checked by nothing — which is exactly the case where Room's runtime validation will kill
the app on launch if the type, nullability or default disagrees by a character.

This matters most for `reviewed`, whose `DEFAULT` has to match the entity's `@ColumnInfo`
declaration exactly.

**Fix:** for any `ADD COLUMN` statement, look the column up in the to-version schema's entity
JSON (`fields[].columnName`, `affinity`, `notNull`, `defaultValue`) and compare. This is a small
addition and it closes the only remaining way this migration can crash on launch.

### 1d. `PRE_EXISTING_TABLES` is a hardcoded V0.1.0 list

The finance tables now hold real money and are not in it, so the checker would happily let a
future migration `DELETE FROM finance_transactions`.

**Fix:** derive the protected set from the committed from-version schema (§1a) rather than a
literal, and keep the literal only as the fallback for v1. Also parameterise the CI step, which
currently hardcodes `1 2` and would silently keep checking the old migration forever.

---

## 2. Scheduled payments — new table

What the design shows on Home and on Finance → Overview: a dated list of money that is going to
leave, before it has.

`finance_scheduled_payments`

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT NOT NULL | PK, UUID |
| `name` | TEXT NOT NULL | "Lui Loan", "Dhiraagu internet" |
| `amountMinor` | INTEGER NOT NULL | Always positive, like `finance_transactions` |
| `currency` | TEXT NOT NULL | |
| `accountId` | TEXT | FK → `finance_accounts`, `SET NULL` |
| `destinationAccountId` | TEXT | FK → `finance_accounts`, `SET NULL`; transfers only |
| `categoryId` | TEXT | FK → `finance_categories`, `SET NULL` |
| `loanId` | TEXT | FK → `finance_loans`, `SET NULL` |
| `dueOn` | TEXT NOT NULL | `LocalDate`, stored as ISO text like every other date |
| `isAutomatic` | INTEGER NOT NULL | The design's "AUTO" tag — a standing order the bank runs |
| `status` | TEXT NOT NULL | `PENDING` / `PAID` / `SKIPPED` / `CANCELLED` |
| `paidTransactionId` | TEXT | FK → `finance_transactions`, `SET NULL` — the payment that settled it |
| `note` | TEXT NOT NULL | |
| `createdAt` | TEXT NOT NULL | |

Indices: `accountId`, `destinationAccountId`, `categoryId`, `loanId`, `paidTransactionId`,
`dueOn`, `status`.

Every foreign key is `SET NULL`, deliberately. A scheduled payment is a statement of intent; it
should survive the account or category it referred to being deleted, the same way a transaction
survives its category being removed. `NO_ACTION` here would make deleting an account fail for a
reason the user cannot see.

**One row per occurrence, no recurrence field.** "Pay the loan on the 7th of every month" is a
recurrence, and the roadmap holds recurrence back until it has a model — what a repeat *is*, what
happens when one is missed, whether history is per-occurrence. Putting a `recurrence` column here
would be guessing at that model from the finance side, and a wrong guess is a further migration.
Rows are created individually until that design exists; generating them is then a matter of
writing rows, not of changing the schema.

`paidTransactionId` is what stops a scheduled payment double-counting. Home shows `PENDING` rows
as money still to leave; once settled the row points at the real transaction and drops out.

---

## 3. Transaction review — one column

`ALTER TABLE finance_transactions ADD COLUMN reviewed INTEGER NOT NULL DEFAULT 1`

Entity: `@ColumnInfo(defaultValue = "1") val reviewed: Boolean = false`

This is an attribute of the transaction, so it belongs on the transaction. The alternative — a
`finance_transaction_reviews` side table — avoids `ALTER TABLE` but models a one-to-one as a
one-to-many and puts a join in front of every transaction query, to dodge a tooling limitation
that §1b fixes properly in about five lines.

### The default is the decision worth arguing about

`DEFAULT 1`, meaning **everything already in the database counts as reviewed**.

The opposite (`DEFAULT 0`) is tempting, because "reviewed" really means "the user has confirmed
this transaction's classification" and plenty of existing rows have no category. But the database
now holds weeks of real transactions, and defaulting them to unreviewed means the first launch
after the upgrade opens on a review queue dozens long, for spending the user has already been
living with. That reads as the app being broken, and a "needs attention" list that is mostly
history is a list nobody reads.

So: history is grandfathered, and the flag starts doing its job on transactions recorded from
here on. New transactions are inserted with `reviewed = false` unless created with a category
already chosen — which is what Finance Quick Capture does, so a capture that resolves at entry is
never busywork later.

The declared default and the migration's `DEFAULT` must match character for character or Room
throws on launch. §1c is what makes that checkable instead of hoped for.

---

## 4. Activity history — new table, and the one with real design left in it

`activity_entries`

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT NOT NULL | PK, UUID |
| `entityType` | TEXT NOT NULL | `TASK` / `PROJECT` / `NOTE` / `WAITING` / `TRANSACTION` / `ACCOUNT` / `GOAL` / `LOAN` / `INBOX` |
| `entityId` | TEXT NOT NULL | **Not a foreign key** — see below |
| `action` | TEXT NOT NULL | `CREATED` / `UPDATED` / `COMPLETED` / `REOPENED` / `DELETED` / `MOVED` |
| `field` | TEXT | Which field changed, when `action` is `UPDATED` |
| `oldValue` | TEXT | Display text, not a typed value |
| `newValue` | TEXT | |
| `summary` | TEXT NOT NULL | The pre-rendered line the peek sheet shows |
| `occurredAt` | TEXT NOT NULL | |

Indices: `(entityType, entityId)` composite, `occurredAt`.

### Three things this table cannot solve on its own

**It cannot have a foreign key.** `entityId` points into nine different tables, and SQLite
foreign keys reference exactly one. So the database cannot enforce that an entry refers to
something real, and deleting a task leaves its history behind as orphans. That is arguably
correct — "what happened to the thing you deleted" is often the question being asked — but it is a
policy, and it needs stating rather than falling out of a limitation.

**The write path is the actual work.** Every repository mutation has to record an entry, and a
mutation that forgets leaves a silent hole in a history that looks complete. Doing it by hand in
each repository method is the version that rots. The alternatives — a decorator around each
repository, or recording at the DAO level — are a design in themselves, and that design, not the
table, is why this was deferred in V0.1.1.

**It grows without limit.** A local-first database with a row per edit needs a retention rule
(cap per entity, or age out beyond a horizon) or it becomes the largest table in the app. No rule
is needed to ship the table; one is needed before the writers are switched on.

### Why the table should still land in this migration

None of the above requires the schema to change again. Adding the table now costs one `CREATE
TABLE` in a migration that is being written and tested anyway, and lets the write path arrive
incrementally afterwards without a second upgrade over real data. The whole reason to batch these
three is that migrations are the risky part; deferring this one because its *feature* is
unfinished would trade a cheap risk now for an expensive one later.

---

## 5. Deliberately not in this migration

- **Areas of Life and Goals.** The roadmap reserves V0.2B for these and they are a larger model
  question (do projects belong to one area, do goals nest). They can share a later migration, or
  join this one if V0.2B is being designed at the same time — but they should not be invented
  here to save a migration.
- **Inbox attachments.** Needs a decision about where files live on disk before a column can
  describe them.
- **Recurrence**, for the reason in §2.
- **Anything that rewrites an existing row.** Every statement here is `CREATE TABLE`, `CREATE
  INDEX`, or `ADD COLUMN`. No `UPDATE`, no backfill pass, nothing that reads user data.

---

## 6. Order of work

| # | Step | Why it is here |
|---|---|---|
| 1 | Commit `app/schemas/`, un-ignore it, add `2.json` | Nothing after this can be verified without it |
| 2 | Narrow `ALTER TABLE` to permit `ADD COLUMN`; verify added columns; derive protected tables from the from-version schema; parameterise the CI step | §1b–1d, all in `verify_room_migration.py` |
| 3 | Mutation-test the checker against a deliberately wrong `ADD COLUMN` | The 1 → 2 checker was mutation-tested before being trusted; this one should be too |
| 4 | Write the three entities and bump `RosDatabase` to 3 | Room then exports `3.json` |
| 5 | Write `MIGRATION_2_3` against that exported schema | |
| 6 | Wire DAOs and repositories; leave the activity write path for its own change | |
| 7 | Upgrade test on the phone, over real data | The 1 → 2 rule stands: CI proves a migration against the schema, not against the database it will run on |

Steps 1–3 are the ones that will be tempting to skip. Skipping them means the migration cannot be
checked, which is the position 1 → 2 deliberately engineered its way out of.

## 7. Test plan

- Unit: the scheduled-payment status transitions and the `paidTransactionId` link, as pure
  functions where possible — the same shape as `FinanceDeletion`.
- CI: the migration checker passes, including the new column check.
- CI: mutation tests — a wrong affinity, a wrong default, a missing index, a smuggled `DROP
  COLUMN` — each must fail the check.
- Device: install over a V0.1.3 build holding real data; open twice; confirm every existing
  project, task, waiting item, note, inbox item, review, account, transaction, goal and loan is
  intact, and that no transaction has appeared in a review queue.
