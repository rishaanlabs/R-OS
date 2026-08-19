# V0.1.2 Manual Test Checklist — Finance

Run on a real Android phone.

**This is the first release that changes the database.** Everything before V0.1.2 only added
screens; this one adds seven tables and moves the schema from version 1 to version 2. Section 0
therefore matters more here than it has in any previous release — do it first, and stop if it
fails.

---

## 0. The upgrade — do this before anything else

Room checks the database against the app's expectations the first time it opens after an upgrade.
If the migration were wrong, the app would crash on launch, on the database holding all your real
data. CI now verifies the migration against the schema Room generates, and it passes — but CI
cannot run it against *your* database. This is that test.

1. Make sure the phone currently has **V0.1.1 installed with your real data**.
2. Download the V0.1.2 APK (README → Installing a Test Build on Android).
3. **Do not uninstall V0.1.1.** Tap the APK and install over the top.
4. Android should offer **Update**, not Install.
5. Open Rishaan OS.

- [ ] The app opens without crashing
- [ ] It does not crash on second launch either (Room validates on first open after upgrade)
- [ ] Every project is still present
- [ ] Every task is still present, completed ones still completed
- [ ] Waiting items are present with follow-up dates intact
- [ ] Notes are present with bodies intact
- [ ] Unprocessed inbox items are still unprocessed
- [ ] Daily reviews are still present
- [ ] Home still shows the right priorities, attention items and inbox count

**If the app crashes on launch, stop and report it.** Do not uninstall to "fix" it — the database
is still intact and recoverable, and uninstalling is what would actually lose the data.

---

## 1. Finance is reachable and empty-safe

- [ ] **More → Finance** opens
- [ ] With nothing set up, no tab crashes: Overview, Accounts, Plan, Goals, Activity, Debt
- [ ] Default expense categories are present
- [ ] Bottom navigation is unchanged — Home, Projects, +, Tasks, More

---

## 2. Accounts

- [ ] A bank account can be created with an opening balance
- [ ] Cash, savings and wallet account types can be created
- [ ] Balance shows the opening balance before any transaction
- [ ] Balance updates after income
- [ ] Balance updates after an expense
- [ ] A transfer decreases the source and increases the destination by the same amount
- [ ] A transfer does not change the combined total across both accounts

---

## 3. Money handling

Amounts are stored as whole minor units, so these should be exact, not approximately right.

- [ ] Entering `1234.56` shows as `1,234.56`, not `1,234.55` or `1,234.5600001`
- [ ] `0.10` + `0.20` recorded as two expenses totals exactly `0.30`
- [ ] A large amount such as `999999.99` is accepted and displays correctly
- [ ] Rubbish input (letters, empty, `1.2.3`) is rejected without crashing
- [ ] A negative or zero amount is handled sensibly

---

## 4. Expenses, categories and the monthly plan

- [ ] An expense can be recorded against a category
- [ ] Categories are marked essential or non-essential
- [ ] A monthly limit can be set on a category and is editable
- [ ] Plan shows spend against limit for the current month
- [ ] Current month versus previous month comparison is correct
- [ ] An expense dated last month does not count towards this month

---

## 5. Savings goals

- [ ] An emergency, medical, travel, study and custom goal can each be created
- [ ] Money can be allocated to a goal
- [ ] **Allocating to a goal does not show up as spending** — this is the point of virtual
      allocation; the money is earmarked, not gone
- [ ] Progress percentage is right (allocate half the target, expect 50%)
- [ ] A planned monthly contribution produces a sensible projected completion date
- [ ] With a target date set, the required monthly saving is right
- [ ] Emergency-fund runway shows in months and is plausible against essential spending
- [ ] A goal already at or past target shows 100%, not more

---

## 6. Loans and debt

- [ ] A loan can be created with principal, interest rate and minimum payment
- [ ] A payment splits into principal, interest and fees
- [ ] Interest paid accumulates across payments
- [ ] Remaining balance decreases by the principal portion only
- [ ] The payoff projection is plausible
- [ ] An extra payment shortens the projection and shows interest saved
- [ ] Avalanche ordering puts the highest interest rate first
- [ ] Snowball ordering puts the smallest balance first
- [ ] A payment that would not cover the interest is handled without hanging or absurd output

---

## 7. Regression — the rest of the app still works

- [ ] Capture still saves to the Inbox
- [ ] Inbox Process still converts to Task, Waiting and Note
- [ ] Task completion works
- [ ] Project next action works
- [ ] Needs Attention still appears on Home
- [ ] Light and dark mode both render Finance correctly
- [ ] Close and reopen — finance data persists
- [ ] Restart the phone — finance data persists
