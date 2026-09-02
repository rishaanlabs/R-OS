# UX Decision Record

Why Rishaan OS is shaped the way it is. This exists so that future work strengthens the product's
character instead of slowly sanding it down into another task manager.

Every decision below is written as a principle plus the reasoning behind it. If a future change
contradicts one of these, that is allowed — but the principle should be argued with, not quietly
ignored.

---

## The through-line

Rishaan OS is a personal execution and memory system. It should be able to follow a chain:

```
What I wanted → What I decided → What project resulted → What I did
    → Who I am waiting on → What changed → What needs attention → What happens next
```

Individual screens are redesigned in service of that chain. A screen that looks better but breaks
a link in it is a regression.

---

## Home — attention first, information second

**Decision.** Home is a briefing, ordered: today's chosen priorities → what needs attention →
what is unprocessed → everything else.

**Why.** A list of everything due is not a plan. The old Home was several equally-weighted cards,
which meant the user had to do the prioritising themselves every time they opened the app. The
new order is an argument about what matters, and a user who reads only the first screenful should
already know what their day is.

**Consequence.** Top priorities get real visual weight — a card, not a row. This is deliberate
and should not be "tidied up" into a uniform list later.

---

## Top 3 — chosen, not computed

**Decision.** Top priorities are what the user picked, capped at three, and never inferred from
the priority field or the due date.

**Why.** "High priority" answers *how important is this*. Top 3 answers a different question:
*if today falls apart, what do I still want done?* Those come apart constantly — an unimportant
task with a hard deadline belongs in the day; an important task with no urgency often does not.
Computing one from the other would destroy the distinction.

**Consequence.** 0 to 3 is valid. Zero is not an error state; it means the day has not been
planned yet, and the empty state says exactly that.

---

## Capture — capture first, classify later

**Decision.** Capture takes text and nothing else. Type is optional and hidden until asked for.
No field is required.

**Why.** Requiring a classification at capture time means the user must understand a thought
before they are allowed to write it down. That is precisely when they are least able to — the
thought arrived mid-conversation, mid-commute, mid-something. Any friction here costs a captured
thought entirely, and a thought that never got captured is worse than one that is filed wrongly.

**Consequence.** The Inbox has to be good, because Capture deliberately pushes work into it.

**Deliberately not done.** Date and project pickers at capture time. `InboxItem` has no column
for either, so supporting them would mean a schema migration on a database that already holds
real user data — for information that is a *classification* decision and therefore belongs to
processing anyway. Revisit if a migration is needed for other reasons.

---

## Finance capture — the one place classification happens at entry

**Decision.** Finance Quick Capture resolves fully at entry. `Coffee 45` writes a real expense
against a real account and category; it never lands in the Inbox as an unresolved amount. This
is a deliberate exception to *capture first, classify later*, and the only one.

**Why the principle does not apply here.** That principle protects a thought whose shape is not
yet known — the user does not yet understand what they captured, so making them classify it
costs the capture. A spend is the opposite: it has already happened, the amount is known, and
the account it came from is known at the moment it is typed. There is no understanding still to
arrive. Deferring it does not protect anything; it just means the same two facts get supplied
later, from memory, with less certainty than the user had at the till.

**Why it is not merely convenient.** A half-formed money object is worse than a half-formed
thought. An unclassified inbox item is a note that reads oddly until processed. An unclassified
expense is a balance that is silently wrong — the account it came out of has not moved, so every
figure downstream (this month versus last, category limits, runway) is quoting a number that
does not match reality. Finance is the one area where an unprocessed item corrupts the answers
other screens are giving.

**Consequence — resolution must not read as a form.** Fast is still the requirement; the
exception buys correctness, not the right to be slow. `Coffee 45` should parse to amount 45 with
"Coffee" matched against existing categories and recent descriptions, leaving account and
category as two taps against sensible defaults, not an empty transaction screen. If capture ever
degrades into filling in fields, the exception has failed and is worth revisiting — but the fix
is a better resolution step, not an unresolved expense.

**Consequence — no schema debt.** Because it resolves at entry, it writes only to the finance
tables V0.1.2 already created. Parking an unresolved amount would need an amount column on
`inbox_items` — a migration. The product argument and the schema argument reach the same place,
which is why this is settled rather than provisional.

---

## Inbox — process, do not browse

**Decision.** The Inbox opens in Process mode: one item, one question, straight to the next.
List mode exists but is secondary.

**Why.** An Inbox rendered as a list becomes a second task list that is never emptied — the user
scrolls it, feels bad, and closes it. Presenting one item at a time makes the decision small and
finite, and showing the remaining count makes finishing feel reachable.

**Consequence.** The metadata offered is scoped to the destination. A Waiting item asks who you
are waiting on; a Task does not. Fields that cannot apply are never rendered.

---

## Waiting — first class, never a task status

**Decision.** Waiting stays its own entity with its own follow-up date and resolution.

**Why.** Waiting means *I have done my part; someone else owns the next action.* Collapsing that
into a task status loses the only thing that makes it useful — that the ball is not in your court,
and that there is a date on which you should chase it. This is one of the few concepts that
genuinely distinguishes Rishaan OS from a to-do list.

---

## Project — context and next action before task volume

**Decision.** A project page leads with outcome and next action. Tasks, waiting items and notes
sit behind summary rows.

**Why.** A project with thirty tasks must still answer "what moves this forward?" in one glance.
If the task list occupies the screen, the answer is buried in it.

**Consequence.** Progress is shown as counts, and the bar is captioned "tasks closed, not outcome
reached". A percentage implying the *outcome* is 61% complete because most tasks are ticked is a
claim the data cannot support, and inventing it would teach the user to distrust the number.

---

## Next action — resolved, never assumed

**Decision.** A next action pointing at a task that has been completed or deleted is treated as
missing, and completing the next action clears the pointer.

**Why.** A stale pointer is worse than an empty one. An empty next action asks to be filled; a
stale one looks answered while being wrong, and the project drifts while appearing healthy.

---

## Needs Attention — deterministic interpretation before AI

**Decision.** A rule-based engine reads current state and produces attention items. It is a pure
function with the date passed in, so every rule is unit-tested.

**Why.** The app should be able to say *something has gone wrong without you* long before it can
say anything clever. Deterministic rules are explainable, testable, and never wrong in a way the
user cannot understand. When intelligence arrives later, it should sit on top of this layer, not
replace it.

**Consequence.** No new tables. Everything is derived from state that already exists, so the
interpretation is always consistent with the data and cannot go stale.

---

## Severity — a briefing, not an alarm

**Decision.** Three severities. Only URGENT may use the error colour, and severity is shown as a
small leading dot rather than a coloured card.

**Why.** If everything is red, nothing is. An app that greets the user with a wall of warnings
gets closed. Most things the system notices deserve a mention, not an alarm — so an unprocessed
inbox is informational, a project with no next action is a nudge, and only genuinely
time-sensitive overdue work is allowed to shout.

---

## Empty states — normal, not broken

**Decision.** An empty day reads "Your day is clear." An empty inbox reads "Inbox clear."
Neither is styled as a problem or a celebration.

**Why.** A clear day is a legitimate outcome, not a failure to plan. Illustrations and
congratulation animations would make an ordinary state feel like an event, which gets tiring by
the third time.

---

## Architecture — one state object per screen

**Decision.** Screens observe a single `stateIn` StateFlow of an immutable UI state. Business
rules live in the domain or ViewModel layer, never in Composables.

**Why.** Home reads from five tables. Exposed as five flows, the screen assembled itself in
pieces as each one arrived and visibly flickered. One combined state means the screen either has
a briefing or it does not. It also keeps rules like "is this waiting item overdue" out of the UI,
where they cannot be tested.

---

## Data safety outranks visual completeness

**Decision.** V0.1.1 changes no database schema. Where a design idea needed a schema change, the
idea was cut or simplified rather than the schema migrated.

**Why.** The owner is testing with real data on a real phone. A visual improvement is worth far
less than not losing that data. Two things were cut on these grounds — capture-time date and
project, and a full activity log — and both are recorded in the roadmap rather than dropped.
