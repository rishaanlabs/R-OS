package com.rishaanlabs.ros.ui.screen.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
// ExposedDropdownMenu is a member of ExposedDropdownMenuBoxScope rather than a top-level
// function, so it resolves inside the box's lambda and must not be imported by name.
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.data.local.entity.Project
import com.rishaanlabs.ros.data.local.entity.ProjectStatus
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.ui.components.AttentionCard
import com.rishaanlabs.ros.ui.components.EmptyState
import com.rishaanlabs.ros.ui.components.ProjectSummaryRow
import com.rishaanlabs.ros.ui.components.SectionHeader
import com.rishaanlabs.ros.ui.components.TaskRow

/**
 * A project is a context, not a task list.
 *
 * Opening one should answer, in this order: what are we trying to achieve, what moves it forward
 * next, is anything stuck, how much is left, and what else is attached. The task list is
 * genuinely useful but it is the *last* of those questions, so it sits behind a summary row
 * rather than occupying the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String?,
    onBack: () -> Unit,
    onNavigateToTask: (String) -> Unit,
    onNavigateToNote: (String) -> Unit,
    onNavigateToWaiting: (String) -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ProjectStatus.ACTIVE) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(projectId == null) }
    var choosingNextAction by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.project) {
        state.project?.let { project ->
            if (title.isEmpty()) title = project.title
            if (description.isEmpty()) description = project.description
            status = project.status
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (projectId == null) "New project" else title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.project != null && !isEditing) {
                        IconButton(onClick = { isEditing = true }) { Icon(Icons.Default.Edit, "Edit") }
                        IconButton(onClick = { viewModel.delete() }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                    if (isEditing) {
                        TextButton(onClick = { viewModel.save(title, description, status) }) { Text("Save") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (isEditing) {
                ProjectEditForm(
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    status = status,
                    onStatusChange = { status = it },
                    showStatusMenu = showStatusMenu,
                    onShowStatusMenuChange = { showStatusMenu = it },
                    showStatus = projectId != null
                )
            } else {
                state.project?.let { project ->
                    ProjectContext(
                        project = project,
                        state = state,
                        expandedSection = expandedSection,
                        onExpandSection = { expandedSection = if (expandedSection == it) null else it },
                        onChooseNextAction = { choosingNextAction = true },
                        onNavigateToTask = onNavigateToTask,
                        onNavigateToNote = onNavigateToNote,
                        onNavigateToWaiting = onNavigateToWaiting,
                        onCompleteTask = viewModel::completeTask
                    )
                }
            }
        }
    }

    if (choosingNextAction) {
        ChooseNextActionDialog(
            candidates = state.openTasks,
            current = state.nextAction?.id,
            onChoose = {
                viewModel.setNextAction(it)
                choosingNextAction = false
            },
            onDismiss = { choosingNextAction = false }
        )
    }
}

@Composable
private fun ProjectContext(
    project: Project,
    state: ProjectDetailUiState,
    expandedSection: String?,
    onExpandSection: (String) -> Unit,
    onChooseNextAction: () -> Unit,
    onNavigateToTask: (String) -> Unit,
    onNavigateToNote: (String) -> Unit,
    onNavigateToWaiting: (String) -> Unit,
    onCompleteTask: (Task) -> Unit
) {
    // ------------------------------------------------------------------ outcome
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
        SuggestionChip(
            onClick = {},
            label = { Text(project.status.name.lowercase().replaceFirstChar { it.uppercase() }) }
        )
        if (project.description.isNotBlank()) {
            Text(
                text = "OUTCOME",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    // -------------------------------------------------------------- next action
    SectionHeader(title = "Next action", modifier = Modifier.padding(top = 16.dp))
    val nextAction = state.nextAction
    if (nextAction != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            onClick = { onNavigateToTask(nextAction.id) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            TaskRow(
                title = nextAction.title,
                onClick = { onNavigateToTask(nextAction.id) },
                onToggleComplete = { onCompleteTask(nextAction) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    } else if (state.needsNextAction) {
        EmptyState(
            message = "No next action selected.",
            supporting = "Choose the task that moves this project forward.",
            actionLabel = "Choose next action",
            onAction = onChooseNextAction
        )
    } else if (state.openTasks.isNotEmpty()) {
        EmptyState(
            message = "No next action selected.",
            actionLabel = "Choose next action",
            onAction = onChooseNextAction
        )
    } else {
        EmptyState(message = "No open tasks.", supporting = "Nothing is outstanding on this project.")
    }

    // ----------------------------------------------------------------- attention
    if (state.attention.isNotEmpty()) {
        SectionHeader(title = "Needs attention", modifier = Modifier.padding(top = 16.dp))
        state.attention.forEach { item ->
            AttentionCard(item = item)
        }
    }

    // ------------------------------------------------------------------ progress
    val completed = state.completedTasks.size
    val open = state.openTasks.size
    if (completed + open > 0) {
        SectionHeader(title = "Progress", modifier = Modifier.padding(top = 16.dp))
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "$completed completed · $open open",
                style = MaterialTheme.typography.bodyLarge
            )
            // The bar tracks tasks closed, not the outcome achieved. The caption says so, because
            // implying a project is "61% done" because most of its tasks are ticked is a lie.
            LinearProgressIndicator(
                progress = { completed.toFloat() / (completed + open).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            Text(
                text = "Tasks closed, not outcome reached.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    // ------------------------------------------------------------------ sections
    SectionHeader(title = "In this project", modifier = Modifier.padding(top = 16.dp))
    ProjectSummaryRow(label = "Tasks", count = state.openTasks.size, onClick = { onExpandSection("tasks") })
    if (expandedSection == "tasks") {
        state.openTasks.forEach { task ->
            TaskRow(
                title = task.title,
                onClick = { onNavigateToTask(task.id) },
                onToggleComplete = { onCompleteTask(task) }
            )
        }
        if (state.completedTasks.isNotEmpty()) {
            Text(
                text = "${state.completedTasks.size} completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

    ProjectSummaryRow(
        label = "Waiting on",
        count = state.openWaiting.size,
        highlight = true,
        onClick = { onExpandSection("waiting") }
    )
    if (expandedSection == "waiting") {
        state.waitingItems.forEach { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToWaiting(item.id) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                if (item.person.isNotBlank()) {
                    Text(
                        item.person,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

    ProjectSummaryRow(label = "Notes", count = state.notes.size, onClick = { onExpandSection("notes") })
    if (expandedSection == "notes") {
        state.notes.forEach { note ->
            Text(
                text = note.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToNote(note.id) }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

    // -------------------------------------------------------------------- recent
    if (state.recentActivity.isNotEmpty()) {
        SectionHeader(title = "Recent", modifier = Modifier.padding(top = 16.dp))
        state.recentActivity.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = entry.at.toLocalDate()
                        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectEditForm(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    status: ProjectStatus,
    onStatusChange: (ProjectStatus) -> Unit,
    showStatusMenu: Boolean,
    onShowStatusMenuChange: (Boolean) -> Unit,
    showStatus: Boolean
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Outcome") },
            supportingText = { Text("What does done look like?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        if (showStatus) {
            ExposedDropdownMenuBox(
                expanded = showStatusMenu,
                onExpandedChange = onShowStatusMenuChange
            ) {
                OutlinedTextField(
                    value = status.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showStatusMenu,
                    onDismissRequest = { onShowStatusMenuChange(false) }
                ) {
                    ProjectStatus.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onStatusChange(option)
                                onShowStatusMenuChange(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooseNextActionDialog(
    candidates: List<Task>,
    current: String?,
    onChoose: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What moves this forward?") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (candidates.isEmpty()) {
                    Text("There are no open tasks to choose from.")
                } else {
                    candidates.forEach { task ->
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (task.id == current) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChoose(task.id) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (current != null) {
                TextButton(onClick = { onChoose(null) }) { Text("Clear") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
