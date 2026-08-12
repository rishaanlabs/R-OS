package com.rishaanlabs.ros.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.TaskPriority
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToInbox: () -> Unit,
    onNavigateToTask: (String) -> Unit,
    onNavigateToWaiting: () -> Unit,
    onCapture: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle(HomeUiState())
    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onCapture) {
                        Icon(Icons.Default.Add, contentDescription = "Quick Capture")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(
                        text = state.greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Top Priorities
            item {
                SectionHeader(title = "Top Priorities")
            }
            if (state.topPriorityTasks.isEmpty()) {
                item {
                    EmptyHint("No top priorities set. Mark tasks as priority to see them here.")
                }
            } else {
                items(state.topPriorityTasks) { task ->
                    TaskRow(task = task, onClick = { onNavigateToTask(task.id) })
                }
            }

            // Today
            item { SectionHeader(title = "Today") }
            if (state.todayTasks.isEmpty()) {
                item { EmptyHint("Nothing scheduled for today.") }
            } else {
                items(state.todayTasks) { task ->
                    TaskRow(task = task, onClick = { onNavigateToTask(task.id) })
                }
            }

            // Waiting
            item {
                WaitingSummaryCard(
                    count = state.waitingCount,
                    overdueCount = state.waitingOverdueCount,
                    onClick = onNavigateToWaiting
                )
            }

            // Inbox
            item {
                InboxSummaryCard(count = state.inboxCount, onClick = onNavigateToInbox)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun TaskRow(task: Task, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(task.title) },
        supportingContent = if (task.priority != TaskPriority.NONE) {
            { Text(task.priority.name.lowercase().replaceFirstChar { it.uppercase() }) }
        } else null,
        leadingContent = {
            Icon(
                Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
}

@Composable
private fun WaitingSummaryCard(count: Int, overdueCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = if (overdueCount > 0) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = if (overdueCount > 0) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (count == 0) "Nothing waiting" else "$count item${if (count != 1) "s" else ""} waiting",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (overdueCount > 0) {
                    Text(
                        text = "$overdueCount follow-up${if (overdueCount != 1) "s" else ""} overdue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxSummaryCard(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Inbox, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (count == 0) "Inbox is clear" else "$count item${if (count != 1) "s" else ""} to process",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
