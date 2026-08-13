package com.rishaanlabs.ros.ui.screen.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.data.local.entity.InboxItem
import com.rishaanlabs.ros.data.local.entity.Project
import com.rishaanlabs.ros.data.local.entity.TaskPriority
import com.rishaanlabs.ros.ui.components.MetadataChip
import com.rishaanlabs.ros.ui.components.SectionHeader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class InboxMode { PROCESS, LIST }

/**
 * The Inbox exists to make decisions, not to store a second task list.
 *
 * It therefore opens in Process mode whenever there is anything to process: one item at a time,
 * one question ("what is this?"), and straight on to the next. List mode is available for
 * scanning, but it is deliberately the secondary view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(InboxMode.PROCESS) }

    // Once the queue is empty there is nothing to process, so fall back to the list rather than
    // leaving the user staring at a mode that cannot do anything.
    LaunchedEffect(state.loaded, state.unprocessed.isEmpty()) {
        if (state.loaded && state.unprocessed.isEmpty() && state.all.isNotEmpty()) {
            mode = InboxMode.LIST
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbox") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.all.isNotEmpty()) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    SegmentedButton(
                        selected = mode == InboxMode.PROCESS,
                        onClick = { mode = InboxMode.PROCESS },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        enabled = state.unprocessed.isNotEmpty()
                    ) { Text("Process") }
                    SegmentedButton(
                        selected = mode == InboxMode.LIST,
                        onClick = { mode = InboxMode.LIST },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("List") }
                }
            }

            when {
                !state.loaded -> Unit

                state.unprocessed.isEmpty() && state.all.isEmpty() -> InboxZeroState()

                mode == InboxMode.PROCESS && state.current != null -> ProcessMode(
                    item = state.current!!,
                    remaining = state.remaining,
                    total = state.all.size,
                    projects = state.projects,
                    onProcess = { destination, projectId, date, priority, person ->
                        viewModel.process(
                            item = state.current!!,
                            destination = destination,
                            projectId = projectId,
                            date = date,
                            priority = priority,
                            person = person
                        )
                    },
                    onDelete = { viewModel.delete(state.current!!) }
                )

                mode == InboxMode.PROCESS -> InboxZeroState()

                else -> ListMode(
                    items = state.all,
                    onProcessItem = { mode = InboxMode.PROCESS },
                    onDelete = viewModel::delete
                )
            }
        }
    }
}

/**
 * One item, one decision.
 *
 * The destination buttons come first because that is the only question that must be answered.
 * The metadata that appears afterwards is scoped to the destination — a Waiting item needs a
 * person, a Task does not — so the user never scrolls past fields that cannot apply.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessMode(
    item: InboxItem,
    remaining: Int,
    total: Int,
    projects: List<Project>,
    onProcess: (ProcessDestination, String?, LocalDate?, TaskPriority, String) -> Unit,
    onDelete: () -> Unit
) {
    // Keyed on the item so each new item starts from a clean slate rather than inheriting the
    // previous decision.
    var destination by remember(item.id) { mutableStateOf<ProcessDestination?>(null) }
    var projectId by remember(item.id) { mutableStateOf<String?>(null) }
    var date by remember(item.id) { mutableStateOf<LocalDate?>(null) }
    var priority by remember(item.id) { mutableStateOf(TaskPriority.NONE) }
    var person by remember(item.id) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val processed = (total - remaining).coerceAtLeast(0)
        if (total > 0) {
            LinearProgressIndicator(
                progress = { processed.toFloat() / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
        Text(
            text = if (remaining == 1) "1 thing left to process" else "$remaining things left to process",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = item.text, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Captured ${item.createdAt.format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        SectionHeader(title = "What is this?", modifier = Modifier.padding(top = 12.dp))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DestinationChip("Task", ProcessDestination.TASK, destination) { destination = it }
            DestinationChip("Waiting", ProcessDestination.WAITING, destination) { destination = it }
            DestinationChip("Note", ProcessDestination.NOTE, destination) { destination = it }
        }

        if (destination != null) {
            if (destination == ProcessDestination.WAITING) {
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("Waiting on") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (destination != ProcessDestination.NOTE) {
                SectionHeader(
                    title = if (destination == ProcessDestination.WAITING) "Follow up" else "Due"
                )
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateChip("Today", LocalDate.now(), date) { date = it }
                    DateChip("Tomorrow", LocalDate.now().plusDays(1), date) { date = it }
                    DateChip("Next week", LocalDate.now().plusWeeks(1), date) { date = it }
                }
            }

            if (destination == ProcessDestination.TASK) {
                SectionHeader(title = "Priority")
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityChip("Low", TaskPriority.LOW, priority) { priority = it }
                    PriorityChip("Medium", TaskPriority.MEDIUM, priority) { priority = it }
                    PriorityChip("High", TaskPriority.HIGH, priority) { priority = it }
                }
            }

            if (projects.isNotEmpty()) {
                SectionHeader(title = "Project")
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    projects.forEach { project ->
                        FilterChip(
                            selected = projectId == project.id,
                            onClick = {
                                projectId = if (projectId == project.id) null else project.id
                            },
                            label = { Text(project.title) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDelete) { Text("Discard") }
            Button(
                onClick = {
                    destination?.let { onProcess(it, projectId, date, priority, person) }
                },
                enabled = destination != null
            ) { Text("Process") }
        }
    }
}

@Composable
private fun ListMode(
    items: List<InboxItem>,
    onProcessItem: () -> Unit,
    onDelete: (InboxItem) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.isProcessed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetadataChip(
                        text = item.createdAt.format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))
                    )
                    if (item.type != com.rishaanlabs.ros.data.local.entity.InboxItemType.UNSPECIFIED) {
                        MetadataChip(text = item.type.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                    if (item.isProcessed) MetadataChip(text = "Processed")
                }
                if (!item.isProcessed) {
                    Row {
                        TextButton(onClick = onProcessItem) { Text("Process") }
                        TextButton(onClick = { onDelete(item) }) { Text("Discard") }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

/** Understated on purpose — reaching zero is normal, not an achievement to celebrate. */
@Composable
private fun InboxZeroState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Inbox clear.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Everything you've captured has been processed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationChip(
    label: String,
    value: ProcessDestination,
    selected: ProcessDestination?,
    onSelect: (ProcessDestination?) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(if (selected == value) null else value) },
        label = { Text(label) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateChip(
    label: String,
    value: LocalDate,
    selected: LocalDate?,
    onSelect: (LocalDate?) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(if (selected == value) null else value) },
        label = { Text(label) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityChip(
    label: String,
    value: TaskPriority,
    selected: TaskPriority,
    onSelect: (TaskPriority) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(if (selected == value) TaskPriority.NONE else value) },
        label = { Text(label) }
    )
}
