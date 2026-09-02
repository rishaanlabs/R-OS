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

## V0.1.2 — Finance (current)

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
- [ ] Finance entries from Quick Capture

## V0.2 — Depth

- [ ] Areas of Life (group projects by area: Work, Personal, Health, etc.)
- [ ] Goals (link projects to longer-term outcomes)
- [ ] Idea Incubator (capture and develop ideas)
- [ ] Android home-screen widget (quick capture + today summary)
- [ ] Share to R-OS (Android share-sheet target into the Inbox)
- [ ] App long-press shortcut straight into Capture
- [ ] Image and file capture into the Inbox
- [ ] Recurring tasks
- [ ] Routines
- [ ] Voice capture
- [ ] File attachments
- [ ] Improved search (FTS, filters)
- [ ] Project progress indicators
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
