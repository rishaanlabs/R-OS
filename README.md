# Rishaan OS

**Version:** 0.1.1  
**Platform:** Android (API 26+)

Rishaan OS is an Android-first personal operating system — an external OS for your life. It helps you answer five fundamental questions:

1. What is happening in my life?
2. What matters right now?
3. What am I responsible for doing next?
4. What am I waiting on from other people?
5. What information, decisions, and commitments should I not forget?

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| Jetpack Compose + Material 3 | UI |
| Hilt | Dependency injection |
| Room | Local SQLite database |
| Kotlin Coroutines + Flow | Async & reactive data |
| ViewModel | Screen state management |
| Navigation Compose | In-app navigation |
| WorkManager | Background scheduling (reminders) |
| DataStore | User preferences |

---

## Project Structure

```
app/src/main/java/com/rishaanlabs/ros/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAO interfaces
│   │   ├── entity/       # Room entity data classes
│   │   ├── Converters.kt # TypeConverters for Room
│   │   └── RosDatabase.kt
│   └── repository/       # Data access layer (used by ViewModels)
├── di/
│   └── DatabaseModule.kt # Hilt database injection
├── navigation/
│   └── Screen.kt         # Navigation route definitions
├── ui/
│   ├── screen/
│   │   ├── capture/      # Quick Capture sheet
│   │   ├── daily/        # Daily Review
│   │   ├── home/         # Today/Home screen
│   │   ├── inbox/        # Inbox
│   │   ├── notes/        # Notes CRUD
│   │   ├── projects/     # Projects CRUD + detail
│   │   ├── search/       # Global search
│   │   ├── tasks/        # Tasks CRUD + tabs
│   │   └── waiting/      # Waiting items CRUD
│   ├── theme/
│   │   └── Theme.kt      # Material 3 color scheme + dark mode
│   ├── MoreScreen.kt
│   └── RosNavHost.kt     # Navigation graph
├── MainActivity.kt
└── RosApplication.kt     # Hilt application + WorkManager init
```

---

## What's New in V0.1.1

V0.1.1 adds almost nothing to store. It changes how what you already have is presented, so the
app reads less like a database of tasks and more like a briefing of what deserves your attention.

**Home is now a daily briefing.** It opens with your chosen priorities for the day, then what
needs attention, then what you have not processed, then everything else. The order is the point:
if you read only the first screenful, you should already know what your day is.

**Top 3 priorities are chosen, not calculated.** "High priority" answers *how important is this*.
Your Top 3 answers a different question — *if today falls apart, what do I still want done?*
Pick up to three from **Plan my day**. Zero is a valid answer.

**Plan My Day** is a two-step flow: decide what happens to unfinished work (Today / Later /
Someday / Cancel), then choose today's few priorities. No timeboxing or capacity planning.

**Capture asks for text and nothing else.** Tap +, type, save. You are never made to decide what
a thought *is* before you are allowed to write it down. A type chip is there if you already know,
and tapping it again clears it.

**The Inbox processes instead of listing.** It opens on one item with one question — Task,
Waiting or Note — and moves straight to the next, showing how many are left. The follow-up
questions match your answer: a Waiting item asks who you are waiting on, a Task does not. A List
mode is still available for scanning.

**Projects read as context.** A project leads with its outcome and its next action, then anything
blocking it, then progress as plain counts. Tasks, waiting items and notes sit behind summary
rows, so notes are no longer fifty tasks away. If a next action points at a task you have since
completed, the project says it needs a new one rather than quietly showing stale information.

**Needs Attention** is a new deterministic layer — not AI. It reads your current data and tells
you when a follow-up is overdue, a project has no next action, a task has slipped, or a project
has gone quiet. Only genuinely time-sensitive items use a warning colour; the rest stay calm.

### Upgrading from V0.1.0

**Your data is preserved.** V0.1.1 changes no database structure, and the app is signed with the
same key, so it installs straight over V0.1.0 as an ordinary update.

**Do not uninstall V0.1.0 first.** Uninstalling deletes the database and everything in it.

---

## Installing a Test Build on Android

You do not need a computer, Android Studio, a USB cable, ADB, or any command-line tool.
Everything below is done on the phone, in a browser and the Files app.

### 1. Get the APK

1. Open **https://github.com/rishaanlabs/R-OS** in your phone's browser.
2. Tap the **☰ menu** (or scroll across the tab strip) and choose **Actions**.
3. In the left list, tap **Build Android APK**.
4. Tap the **topmost run with a green ✅ tick**. A red ❌ means that build failed — pick
   the newest green one instead.
5. Scroll to the bottom of that page to the **Artifacts** section.
6. Tap **rishaan-os-debug**. It downloads as a `.zip` file.

> On some phones GitHub's mobile site hides the Artifacts section. If you cannot see it,
> tap the browser menu and choose **Desktop site**, then scroll to the bottom again.

### 2. Get the APK out of the ZIP

GitHub always hands out Actions artifacts as a ZIP, so there is one extra step:

7. Open your **Files** app (on Samsung it is called **My Files**) and go to **Downloads**.
8. Tap **rishaan-os-debug.zip**.
9. Choose **Extract** (some file managers say *Unzip* or *Extract all*).
10. Inside you will find **Rishaan-OS-v0.1.1-debug.apk**.

### 3. Install it

11. Tap **Rishaan-OS-v0.1.1-debug.apk**.
12. Android will warn that this app came from outside the Play Store. Tap **Settings** on that
    prompt, then turn on **Allow from this source**, then tap **Back**. You only have to do
    this once per app (your browser or your file manager).
13. Tap **Install**.
14. Tap **Open**. Rishaan OS starts on the Home screen.

If Play Protect says the app is unrecognised, tap **More details → Install anyway**. That is
expected for a personal build that was never submitted to Google.

### 4. Asking for a fresh build from your phone

You do not have to wait for someone to push code:

1. Repository → **Actions** → **Build Android APK**.
2. Tap **Run workflow**, pick the branch, tap the green **Run workflow** button.
3. Wait roughly five minutes, refresh, then follow step 1 above to download the new APK.

### 5. Installing a newer build later

Just install the new APK on top of the old one — **do not uninstall first**. All builds are
signed with the same key that is stored in this repository, so Android treats a new build as an
upgrade and **your tasks, projects, and notes are preserved**. Uninstalling would delete the
database.

If an install is ever refused with *"App not installed"*, that means the signature changed; in
that case uninstall Rishaan OS first, accepting that its data is lost.

### Shortcut: install from Releases instead

Downloading a ZIP and extracting it is fiddly on a phone. There is a second, easier route:

1. Repository → **Actions** → **Publish Test Release APK** → **Run workflow**, enter a version
   such as `0.1.1`, and run it.
2. When it finishes, go to the repository home page → **Releases**.
3. Tap **Rishaan OS v0.1.1 (test build)** and tap the `.apk` file under **Assets**.
4. It downloads as an APK directly — no ZIP, no extracting. Continue from step 11 above.

That workflow only ever runs when you trigger it by hand, and every build it publishes is
marked as a **prerelease** to keep test builds clearly separate from real releases.

---

## Building from a Computer (optional)

If you ever do have a development machine:

1. Open the project in Android Studio (Ladybug or later recommended)
2. Ensure you have a device or emulator running API 26+
3. Click **Run** or use `./gradlew installDebug`

---

## Architecture

Rishaan OS follows standard Android architecture:

```
UI (Compose Screens)
    ↕ StateFlow / collectAsStateWithLifecycle
ViewModels
    ↕ suspend functions + Flow
Repositories
    ↕ Room DAOs (Flow / suspend)
Room Database (local SQLite)
```

- **Local-first**: All data lives on-device. No internet required.
- **UUID identifiers**: All entities use UUID primary keys to support future sync.
- **Forgiving foreign keys**: Deleting a project sets task/note/waiting projectId to NULL (not cascade delete) to preserve user data.
- **Single source of truth**: Room is the source of truth. ViewModels observe reactive Flows.

---

## Implemented Features (V0.1)

- [x] Room database with all V0.1 entities
- [x] Quick Capture (bottom sheet, accessible from anywhere)
- [x] Inbox (capture, process, convert to task/note/waiting)
- [x] Tasks (create, complete, edit, Today/Upcoming/All/Someday/Completed tabs)
- [x] Projects (create, status management, task/note/waiting relationships, next action)
- [x] Waiting Items (create, follow-up dates, overdue surfacing, resolve)
- [x] Notes (create, edit, optional project association, markdown-compatible)
- [x] Home/Today screen (priorities, today tasks, waiting summary, inbox count)
- [x] Daily Review (energy, focus, wins, reflection, tomorrow priority)
- [x] Daily Review History
- [x] Global Search (tasks, notes, waiting items)
- [x] Dark mode support
- [x] Material 3 theme

---

## Planned Features

See [docs/roadmap.md](docs/roadmap.md) for the full roadmap.

---

## Known Issues / Limitations

- No attachment support (V0.2)
- No recurring tasks (V0.2)
- No cloud backup (V0.3)
- No AI assistance (V1.0)
- Search does not include inbox items in V0.1
