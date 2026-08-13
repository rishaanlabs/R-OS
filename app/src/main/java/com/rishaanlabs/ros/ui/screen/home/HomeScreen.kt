package com.rishaanlabs.ros.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.domain.attention.AttentionTarget
import com.rishaanlabs.ros.ui.components.AttentionCard
import com.rishaanlabs.ros.ui.components.EmptyState
import com.rishaanlabs.ros.ui.components.Metadata
import com.rishaanlabs.ros.ui.components.PriorityTaskCard
import com.rishaanlabs.ros.ui.components.SectionHeader
import com.rishaanlabs.ros.ui.components.TaskRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Home is a briefing, not a dashboard.
 *
 * The order of the sections is the argument: what I intend to do today, what has gone wrong
 * without me, what I have not yet decided about, and only then everything else. A user who reads
 * only the first screenful should already know what their day is.
 */
@Composable
fun HomeScreen(
    onNavigateToInbox: () -> Unit,
    onNavigateToTask: (String) -> Unit,
    onNavigateToWaiting: () -> Unit,
    onNavigateToProject: (String) -> Unit = {},
    onCapture: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var planningDay by remember { mutableStateOf(false) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)) {
                    Text(
                        text = state.greeting,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = state.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ------------------------------------------------------------ your day
            item {
                SectionHeader(
                    title = "Your day",
                    trailing = if (state.topPriorities.isNotEmpty()) "Plan" else null,
                    onTrailingClick = { planningDay = true }
                )
                if (state.topPriorities.isNotEmpty() || state.otherTasks.isNotEmpty()) {
                    Text(
                        text = summarise(state.topPriorities.size, state.otherTasks.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                    )
                }
            }

            if (state.topPriorities.isEmpty()) {
                item {
                    if (state.otherTasks.isEmpty() && state.loaded) {
                        EmptyState(
                            message = "Your day is clear.",
                            supporting = "Nothing is scheduled. That is allowed.",
                            actionLabel = "Plan my day",
                            onAction = { planningDay = true }
                        )
                    } else {
                        EmptyState(
                            message = "Nothing chosen yet.",
                            supporting = "Choose what matters most today.",
                            actionLabel = "Plan my day",
                            onAction = { planningDay = true }
                        )
                    }
                }
            } else {
                items(state.topPriorities, key = { "priority-${it.id}" }) { task ->
                    PriorityTaskCard(
                        title = task.title,
                        metadata = metadataFor(task, state.date),
                        overdue = isOverdue(task, state.date),
                        onClick = { onNavigateToTask(task.id) },
                        onToggleComplete = { viewModel.completeTask(task) }
                    )
                }
            }

            // ---------------------------------------------------- needs attention
            if (state.attention.isNotEmpty()) {
                item { SectionHeader(title = "Needs attention", modifier = Modifier.padding(top = 16.dp)) }
                items(state.attention, key = { it.id }) { item ->
                    AttentionCard(
                        item = item,
                        onClick = {
                            when (item.target) {
                                AttentionTarget.TASK -> item.targetId?.let(onNavigateToTask)
                                AttentionTarget.PROJECT -> item.targetId?.let(onNavigateToProject)
                                AttentionTarget.WAITING -> onNavigateToWaiting()
                                AttentionTarget.INBOX -> onNavigateToInbox()
                                AttentionTarget.NONE -> Unit
                            }
                        }
                    )
                }
            } else if (state.loaded) {
                item {
                    SectionHeader(title = "Needs attention", modifier = Modifier.padding(top = 16.dp))
                    Text(
                        text = "Nothing needs attention right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            // -------------------------------------------------------------- inbox
            if (state.inboxCount > 0) {
                item {
                    SectionHeader(title = "Inbox", modifier = Modifier.padding(top = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToInbox)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (state.inboxCount == 1) {
                                "1 thing needs processing"
                            } else {
                                "${state.inboxCount} things need processing"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // -------------------------------------------------------- later today
            if (state.otherTasks.isNotEmpty()) {
                item { SectionHeader(title = "Later today", modifier = Modifier.padding(top = 16.dp)) }
                items(state.otherTasks, key = { "other-${it.id}" }) { task ->
                    TaskRow(
                        title = task.title,
                        metadata = metadataFor(task, state.date),
                        overdue = isOverdue(task, state.date),
                        onClick = { onNavigateToTask(task.id) },
                        onToggleComplete = { viewModel.completeTask(task) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (planningDay) {
        PlanMyDaySheet(
            onDismiss = { planningDay = false },
            viewModel = viewModel
        )
    }
}

private fun summarise(priorities: Int, others: Int): String {
    val parts = mutableListOf<String>()
    if (priorities > 0) parts += if (priorities == 1) "1 priority" else "$priorities priorities"
    if (others > 0) parts += if (others == 1) "1 other task" else "$others other tasks"
    return parts.joinToString(" · ")
}

private fun isOverdue(task: Task, today: LocalDate): Boolean {
    val due = task.dueDate?.toLocalDate() ?: return false
    return due.isBefore(today)
}

/** Only shows what is actually known, so cards stay quiet when there is nothing to say. */
private fun metadataFor(task: Task, today: LocalDate): List<String> {
    val metadata = mutableListOf<String>()
    task.dueDate?.let { metadata += Metadata.dueLabel(it.toLocalDate(), today) }
    return metadata
}
