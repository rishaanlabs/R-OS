package com.rishaanlabs.ros.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.navigation.Screen
import com.rishaanlabs.ros.ui.theme.Ros
import com.rishaanlabs.ros.ui.screen.capture.CaptureSheet
import com.rishaanlabs.ros.ui.screen.home.HomeScreen
import com.rishaanlabs.ros.ui.screen.finance.FinanceScreen
import com.rishaanlabs.ros.ui.screen.inbox.InboxScreen
import com.rishaanlabs.ros.ui.screen.notes.NoteDetailScreen
import com.rishaanlabs.ros.ui.screen.notes.NotesScreen
import com.rishaanlabs.ros.ui.screen.projects.ProjectDetailScreen
import com.rishaanlabs.ros.ui.screen.projects.ProjectsScreen
import com.rishaanlabs.ros.ui.screen.search.SearchScreen
import com.rishaanlabs.ros.ui.screen.tasks.TaskDetailScreen
import com.rishaanlabs.ros.ui.screen.tasks.TasksScreen
import com.rishaanlabs.ros.ui.screen.waiting.WaitingDetailScreen
import com.rishaanlabs.ros.ui.screen.waiting.WaitingListScreen
import com.rishaanlabs.ros.ui.screen.daily.DailyReviewScreen
import com.rishaanlabs.ros.ui.screen.daily.DailyReviewHistoryScreen

/**
 * A root tab in the design's bottom bar. Text only — the design uses weight and ink for state
 * rather than icons, so nothing here carries an ImageVector.
 */
internal data class RosTab(val screen: Screen, val label: String)

internal val rosTabs = listOf(
    RosTab(Screen.Home, "Home"),
    RosTab(Screen.Tasks, "Tasks"),
    RosTab(Screen.Finance, "Finance"),
    RosTab(Screen.Inbox, "Inbox")
)

/**
 * The app shell: a persistent search-and-settings row, the scrolling screen, and the bottom bar.
 *
 * The four tabs are the design's. Projects and the old "More" tab gave up their slots because the
 * design puts money and the inbox where the user actually goes, and nothing is lost: More is
 * reached from the settings control in the top bar and now lists Projects alongside Notes,
 * Waiting, Search and Daily Review.
 *
 * Capture keeps its central place but changes shape. On Home it is a full-width bar that reads
 * "Capture anything…", because Home is where a stray thought arrives; everywhere else it shrinks
 * to a round button so it never competes with the screen's own content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosNavHost(shellViewModel: ShellViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    var showCapture by remember { mutableStateOf(false) }
    val inboxCount by shellViewModel.inboxCount.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onHome = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true
    val onTabRoot = rosTabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true
    }

    fun goTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = Ros.colors.bg,
        topBar = {
            RosTopBar(
                onSearch = { navController.navigate(Screen.Search.route) },
                onSettings = { navController.navigate(Screen.More.route) }
            )
        },
        bottomBar = {
            if (onTabRoot) {
                RosBottomBar(
                    tabs = rosTabs,
                    currentDestination = currentDestination,
                    onSelect = { goTo(it.route) },
                    inboxCount = inboxCount
                )
            }
        },
        // No shell-level capture button. The design puts one on every non-Home tab because in
        // that design capture is the only way to add anything; here Tasks and Finance still have
        // their own primary actions, and a second floating button beside them would be two
        // buttons competing for the same corner. Home keeps the capture bar, which is the part of
        // the design that matters — capture is prominent where a stray thought actually arrives.
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToInbox = { navController.navigate(Screen.Inbox.route) },
                    onNavigateToTask = { navController.navigate(Screen.TaskDetail.withId(it)) },
                    onNavigateToWaiting = { navController.navigate(Screen.WaitingList.route) },
                    onNavigateToProject = { navController.navigate(Screen.ProjectDetail.withId(it)) },
                    onNavigateToFinance = { goTo(Screen.Finance.route) },
                    onCapture = { showCapture = true }
                )
            }
            composable(Screen.Projects.route) {
                ProjectsScreen(
                    onProjectClick = { navController.navigate(Screen.ProjectDetail.withId(it)) },
                    onCreateProject = { navController.navigate(Screen.ProjectDetail.withId("new")) }
                )
            }
            composable(Screen.Tasks.route) {
                TasksScreen(
                    onTaskClick = { navController.navigate(Screen.TaskDetail.withId(it)) },
                    onCreateTask = { navController.navigate(Screen.TaskDetail.withId("new")) }
                )
            }
            composable(Screen.More.route) {
                MoreScreen(
                    onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                    onNavigateToInbox = { navController.navigate(Screen.Inbox.route) },
                    onNavigateToNotes = { navController.navigate(Screen.Notes.route) },
                    onNavigateToWaiting = { navController.navigate(Screen.WaitingList.route) },
                    onNavigateToFinance = { navController.navigate(Screen.Finance.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToDailyReview = { navController.navigate(Screen.DailyReview.route) }
                )
            }
            composable(Screen.Finance.route) {
                FinanceScreen()
            }
            composable(Screen.Inbox.route) {
                InboxScreen()
            }
            composable(Screen.ProjectDetail.route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("projectId") ?: "new"
                ProjectDetailScreen(
                    projectId = if (id == "new") null else id,
                    onBack = { navController.popBackStack() },
                    onNavigateToTask = { navController.navigate(Screen.TaskDetail.withId(it)) },
                    onNavigateToNote = { navController.navigate(Screen.NoteDetail.withId(it)) },
                    onNavigateToWaiting = { navController.navigate(Screen.WaitingDetail.withId(it)) }
                )
            }
            composable(Screen.TaskDetail.route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("taskId") ?: "new"
                TaskDetailScreen(
                    taskId = if (id == "new") null else id,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Notes.route) {
                NotesScreen(
                    onNoteClick = { navController.navigate(Screen.NoteDetail.withId(it)) },
                    onCreateNote = { navController.navigate(Screen.NoteDetail.withId("new")) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.NoteDetail.route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("noteId") ?: "new"
                NoteDetailScreen(
                    noteId = if (id == "new") null else id,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WaitingList.route) {
                WaitingListScreen(
                    onItemClick = { navController.navigate(Screen.WaitingDetail.withId(it)) },
                    onCreateItem = { navController.navigate(Screen.WaitingDetail.withId("new")) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WaitingDetail.route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("waitingId") ?: "new"
                WaitingDetailScreen(
                    waitingId = if (id == "new") null else id,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToTask = { navController.navigate(Screen.TaskDetail.withId(it)) },
                    onNavigateToNote = { navController.navigate(Screen.NoteDetail.withId(it)) },
                    onNavigateToProject = { navController.navigate(Screen.ProjectDetail.withId(it)) }
                )
            }
            composable(Screen.DailyReview.route) {
                DailyReviewScreen(
                    onBack = { navController.popBackStack() },
                    onHistory = { navController.navigate(Screen.DailyReviewHistory.route) }
                )
            }
            composable(Screen.DailyReviewHistory.route) {
                DailyReviewHistoryScreen(onBack = { navController.popBackStack() })
            }
            }

            // Home's capture affordance is a bar rather than a button, sitting just above the
            // bottom bar where a thumb already is.
            if (onHome) {
                RosCaptureBar(
                    onClick = { showCapture = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }
    }

    if (showCapture) {
        CaptureSheet(onDismiss = { showCapture = false })
    }
}
