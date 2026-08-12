# Rishaan OS

**Version:** 0.1.0  
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

## How to Run

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
