package com.rishaanlabs.ros.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.rishaanlabs.ros.navigation.Screen
import com.rishaanlabs.ros.ui.screen.capture.CaptureSheet
import com.rishaanlabs.ros.ui.screen.home.HomeScreen
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

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosNavHost() {
    val navController = rememberNavController()
    var showCapture by remember { mutableStateOf(false) }

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home, "Home", Icons.Default.Home),
        BottomNavItem(Screen.Projects, "Projects", Icons.Default.Folder),
        BottomNavItem(Screen.Tasks, "Tasks", Icons.Default.CheckCircle),
        BottomNavItem(Screen.More, "More", Icons.Default.Menu)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // First two items
                bottomNavItems.take(2).forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // Central Capture FAB-style nav item
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Capture") },
                    label = { Text("Capture") },
                    selected = false,
                    onClick = { showCapture = true }
                )

                // Last two items
                bottomNavItems.takeLast(2).forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToInbox = { navController.navigate(Screen.Inbox.route) },
                    onNavigateToTask = { navController.navigate(Screen.TaskDetail.withId(it)) },
                    onNavigateToWaiting = { navController.navigate(Screen.WaitingList.route) },
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
                    onNavigateToInbox = { navController.navigate(Screen.Inbox.route) },
                    onNavigateToNotes = { navController.navigate(Screen.Notes.route) },
                    onNavigateToWaiting = { navController.navigate(Screen.WaitingList.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToDailyReview = { navController.navigate(Screen.DailyReview.route) }
                )
            }
            composable(Screen.Inbox.route) {
                InboxScreen(
                    onBack = { navController.popBackStack() }
                )
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
    }

    if (showCapture) {
        CaptureSheet(onDismiss = { showCapture = false })
    }
}
