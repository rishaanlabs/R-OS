package com.rishaanlabs.ros.navigation

/**
 * Sealed class defining all navigation destinations in the app.
 * Routes use simple string identifiers. Arguments are appended via slash or query params.
 */
sealed class Screen(val route: String) {
    // Bottom nav items
    object Home : Screen("home")
    object Projects : Screen("projects")
    object Capture : Screen("capture")
    object Tasks : Screen("tasks")
    object More : Screen("more")

    // Inbox
    object Inbox : Screen("inbox")
    object Finance : Screen("finance")

    // Detail screens
    object ProjectDetail : Screen("project/{projectId}") {
        fun withId(id: String) = "project/$id"
    }
    object TaskDetail : Screen("task/{taskId}") {
        fun withId(id: String) = "task/$id"
    }
    object NoteDetail : Screen("note/{noteId}") {
        fun withId(id: String) = "note/$id"
    }
    object NoteCreate : Screen("note/create")
    object WaitingDetail : Screen("waiting/{waitingId}") {
        fun withId(id: String) = "waiting/$id"
    }
    object WaitingList : Screen("waiting")
    object Notes : Screen("notes")
    object Search : Screen("search")
    object DailyReview : Screen("daily_review")
    object DailyReviewHistory : Screen("daily_review_history")
}
