# V0.1.1 Manual Test Checklist

Run on a real Android phone. The unit tests cover the Needs Attention rules; everything below is
behaviour only a device can confirm.

---

## 0. Data safety — do this first

This is the acceptance criterion that outranks everything else. Do it before testing any feature.

1. Make sure **V0.1.0 is already installed** with real data in it. If it is not, install V0.1.0
   first and create at least: one project, two tasks (one inside the project), one waiting item,
   one note, and one unprocessed inbox capture.
2. Note roughly what exists, so you can tell if anything is missing afterwards.
3. Download the **V0.1.1** APK (see README → Installing a Test Build on Android).
4. **Do not uninstall V0.1.0.** Tap the new APK and install it over the top.
5. Android should say **Update**, not **Install**, and should not warn about replacing the app.
6. Open Rishaan OS.

- [ ] The update installed without asking to uninstall first
- [ ] Every project from before is still present
- [ ] Every task from before is still present, with completed ones still completed
- [ ] Every waiting item is still present, with its follow-up date intact
- [ ] Every note is still present, with its body intact
- [ ] Unprocessed inbox items are still unprocessed
- [ ] Version shows 0.1.1

If any data is missing, stop and report it. Nothing else in this checklist matters more.

---

## 1. Regression — V0.1.0 behaviour that must still work

- [ ] App launches without crashing
- [ ] Capture creates an inbox item
- [ ] Inbox item converts to a Task
- [ ] Task can be completed
- [ ] Project can be created
- [ ] Task can be added to a project
- [ ] Waiting item can be created
- [ ] Note can be created
- [ ] Close the app fully and reopen — everything is still there
- [ ] Restart the phone and reopen — everything is still there
- [ ] Light mode renders correctly
- [ ] Dark mode renders correctly
- [ ] Bottom navigation still reaches Home, Projects, Capture, Tasks, More

---

## 2. Home

- [ ] Greeting matches the time of day, and the date is correct
- [ ] Top priorities are visually dominant — clearly larger than other tasks
- [ ] Other tasks are still reachable below
- [ ] A task never appears in both the priority and the "Later today" section
- [ ] Needs Attention shows real problems (see section 5 for how to create one)
- [ ] Inbox section shows the correct count and opens the Inbox
- [ ] Tapping an attention item goes somewhere sensible
- [ ] With no priorities chosen: "Nothing chosen yet" with a **Plan my day** action
- [ ] With nothing scheduled at all: "Your day is clear." — and it does not look like an error
- [ ] With nothing wrong: "Nothing needs attention right now." and it stays small
- [ ] The screen does not visibly flicker or rearrange while loading

---

## 3. Plan My Day

- [ ] Opens from **Plan my day** or the **Plan** action on Your day
- [ ] Step 1 lists unfinished and overdue tasks
- [ ] Today / Later / Someday / Cancel each do what they say
- [ ] Step 1 is skipped when there is nothing unfinished
- [ ] Step 2 lets you choose priorities, showing "n of 3"
- [ ] Choosing a task marks it as a priority on Home
- [ ] Tapping a chosen task again removes it
- [ ] Choosing zero priorities is accepted
- [ ] Done closes the sheet and Home reflects the choices

---

## 4. Capture

- [ ] The **+** in the bottom bar opens Capture
- [ ] The keyboard appears immediately without tapping the field
- [ ] Typing and tapping **Save** takes three interactions total
- [ ] Nothing is required beyond the text — no type, no date, no project
- [ ] **Save** is disabled while the text is empty
- [ ] **Type** reveals the optional chips
- [ ] Tapping a selected type chip again clears it
- [ ] The saved item appears in the Inbox
- [ ] The Save button is reachable one-handed with the keyboard open

---

## 5. Inbox

- [ ] With unprocessed items, the Inbox opens in **Process** mode
- [ ] One item is shown at a time, with its capture time
- [ ] The remaining count and progress bar are correct
- [ ] **Task / Waiting / Note** are offered as the first question
- [ ] Choosing **Waiting** asks who you are waiting on; choosing **Task** does not
- [ ] Choosing **Task** offers a due date and priority
- [ ] Choosing **Note** does not offer a due date
- [ ] A project can be attached when projects exist
- [ ] **Process** creates the right record and moves to the next item
- [ ] The processed item disappears from the queue and the count drops
- [ ] Converting to a Task with a due date in the past produces a Needs Attention item on Home
- [ ] Converting to Waiting with a past follow-up date produces a Needs Attention item on Home
- [ ] A long capture becomes a note with a sensible title, not a 200-character one
- [ ] **Discard** removes an item without creating anything
- [ ] **List** mode shows all captures with timestamps and processed state
- [ ] When the queue empties: "Inbox clear." — understated, not celebratory

---

## 6. Projects

- [ ] Project opens showing status and outcome
- [ ] **Next action** is the strongest section after the outcome
- [ ] With a next action set, it shows as a focused card
- [ ] With open tasks and no next action: "No next action selected" with **Choose next action**
- [ ] Choosing a next action updates the card immediately
- [ ] Completing the next action clears it rather than leaving stale information
- [ ] A project with no next action appears in Home's Needs Attention
- [ ] Project-level attention shows this project's own overdue tasks and waiting items
- [ ] Progress counts match reality — completed and open
- [ ] The progress caption makes clear it counts tasks, not the outcome
- [ ] Tasks / Waiting on / Notes rows show correct counts and expand
- [ ] Notes are reachable without scrolling through every task
- [ ] **Recent** lists recent completions, notes and waiting items in date order
- [ ] Editing the project still saves title, outcome and status

---

## 7. Look and feel

- [ ] Nothing is red except genuinely overdue or urgent items
- [ ] A screen with several attention items still feels calm
- [ ] Tap targets are comfortable — completion circles are easy to hit
- [ ] Text is legible in dark mode; no grey-on-grey
- [ ] Chips, cards, sheets and dialogs all render correctly in both themes
- [ ] Long project and task titles truncate instead of breaking the layout
