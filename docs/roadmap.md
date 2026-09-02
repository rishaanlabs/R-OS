# Rishaan OS — Roadmap

## V0.1.0 — Foundation (shipped)

Core personal management system for daily use.

- [x] Room database (local-first)
- [x] Quick Capture
- [x] Inbox (capture + process)
- [x] Tasks (CRUD + Today/Upcoming/All/Someday)
- [x] Projects (CRUD + next action + relationships)
- [x] Waiting Items (first-class follow-up tracking)
- [x] Notes (lightweight, markdown-compatible)
- [x] Home/Today screen (daily command center)
- [x] Daily Review (lightweight reflection)
- [x] Global Search
- [x] Dark mode

## V0.1.1 — Core UX (shipped)

Nothing new to store; the same information, presented so it can be acted on.

- [x] Home as a daily briefing — priorities dominant, attention, inbox, then the rest
- [x] Top 3 priorities chosen by the user, not inferred from the priority field
- [x] Plan My Day — review unfinished work, then choose today's few
- [x] Needs Attention: a deterministic rule engine over existing state, unit-tested
- [x] Capture reduced to text plus an optional type; nothing is required
- [x] Inbox split into Process (one item, one decision) and List
- [x] Inbox conversion carries project, date, priority and person
- [x] Project pages lead with outcome, next action and blockers
- [x] Next action validated against real open tasks; stale pointers treated as missing
- [x] Recent activity derived from timestamps already in the schema
- [x] Shared component set so screens stay visually consistent
- [x] No schema change — V0.1.0 databases upgrade in place

### Deferred out of V0.1.1, with reasons

- [ ] **Date and project at capture time** — `InboxItem` has no column for either. Both are
      classification decisions and belong to processing, so this waits until a migration is
      needed for another reason.
- [ ] **Activity history** — `Recent` is currently derived from `completedAt`, `createdAt` and
      `resolvedAt`. Real history (edits, status changes, previous values) needs its own table
      and design; a half-version would have meant migrating a database holding real data.
- [ ] **Plan My Day: durations, capacity, timeboxing** — deliberately out of scope. These need a
      stronger model than V0.1.1 has, and a weak version would make the feature worse.

## V0.1.2 — Finance (merged, awaiting the on-device upgrade test)

Built, merged and green in CI. Not yet declared stable: this is the first release to change
the database, and that is only proved on a phone holding real data. See the gate under V0.2.

- [x] Bank, cash, savings and wallet accounts with calculated balances
- [x] Income, expenses and same-currency transfers
- [x] Expense categories, essential/non-essential split, editable monthly limits
- [x] This month versus last month
- [x] Savings goals with virtual allocation, so earmarking is not spending
- [x] Required monthly saving, projected completion, emergency-fund runway
- [x] Loans with principal/interest/fee breakdown and payoff projections
- [x] Extra-payment what-if with interest saved; avalanche and snowball ordering
- [x] Money held as integer minor units throughout
- [x] Room 1 → 2 migration, additive only, verified against the exported schema in CI

### Deferred from V0.1.2

- [ ] Automatic percentage-based income routing (the allocation model is ready for it)
- [ ] Foreign exchange and multi-currency conversion
- [ ] Investments
- [ ] Bank API syncing
- [ ] Finance entries from Quick Capture — scheduled into V0.2A

## V0.2 — Depth

Split into two phases. V0.2A makes the system easier to reach every day and touches no
schema. V0.2B changes what the system can model, and needs a migration.

**Gate: no new migration is written until the V0.1.2 upgrade test passes on a real phone**
(`docs/test-checklist-v0.1.2-finance.md`, section 0). Migration 1 → 2 is verified against the
schema Room generates, but nothing has yet proved it against a database holding real data.
Stacking migration 2 → 3 on top of an unproven one would mean debugging two migrations at once,
on the only copy of that data.

### V0.2A — Capture and reach (no schema change)

Everything here writes to tables that already exist, so it can be built and shipped while the
V0.1.2 upgrade test is still outstanding.

- [ ] Share to R-OS — Android share-sheet target for text and links, straight into the Inbox
- [ ] Home-screen widget — one-tap capture plus a today summary
- [ ] App long-press shortcuts — New Task, Quick Capture, Add Expense
- [ ] Finance Quick Capture — "Coffee 45" parsed into an expense against a chosen account

Two details that decide the shape of this phase:

- `inbox_items` holds text and a type, nothing else. Sharing **text or a link** fits that column;
  sharing an **image or file** does not, and needs an attachment column or table. Image and file
  capture therefore moves to V0.2B with the other schema work.
- Finance Quick Capture writes a finance transaction directly, against tables V0.1.2 already
  created. Parking a captured amount in the Inbox *unresolved* would need an amount column on
  `inbox_items`, so the capture resolves at entry: account and category are chosen there, and
  what lands is a real expense rather than a half-entry to process later.

### V0.2B — Life architecture (one migration, after the gate)

- [ ] Areas of Life — Finance, Work, Health, Scouts, Personal, Family
- [ ] Goals — a goal belongs to an area; projects support goals; tasks support projects
- [ ] Dashboard evolution — today, attention, goals progressing or stalled, financial position,
      waiting, inbox, upcoming commitments
- [ ] Image and file capture into the Inbox (needs the attachment column above)
- [ ] Project progress indicators
- [ ] Improved search (FTS, filters)

Areas, goals and attachments are one migration, designed together and written once. The
V0.1.2 CI check (`scripts/verify_room_migration.py`) applies unchanged: additive statements
only, nothing that touches a table already holding user data.

### Held back from V0.2, deliberately

- [ ] **Recurring tasks** and **Routines** — these need a recurrence model (what a repeat *is*,
      what happens when one is missed, whether history is per-occurrence) designed before any
      code. Duplicating a task on a timer is the version that looks finished and then quietly
      loses track of what was actually done.
- [ ] Idea Incubator
- [ ] Voice capture
- [ ] Richer reminders (exact alarm, location)
- [ ] Export (JSON / ZIP)

## V0.3 — Sync

- [ ] Supabase backend
- [ ] Account authentication
- [ ] Encrypted backup
- [ ] Multi-device synchronization
- [ ] Web dashboard
- [ ] Calendar integration
- [ ] Cloud file storage

## V1.0 — Personal Intelligence

The AI layer reasons across the user's structured personal data.

- [ ] "What should I focus on today?" — grounded in real projects, priorities, history
- [ ] "What projects have no next action?"
- [ ] "What am I neglecting?"
- [ ] "What decisions did I make about [project]?"
- [ ] "Turn this brain dump into projects and tasks"
- [ ] Pattern recognition from daily reviews
- [ ] Private AI processing options (on-device or trusted cloud)

The app remains valuable without AI. AI enhances, not replaces, the system.
