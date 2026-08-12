package com.rishaanlabs.ros.ui.screen.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.data.local.entity.ProjectStatus
import com.rishaanlabs.ros.data.local.entity.Task

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

    LaunchedEffect(state.project) {
        state.project?.let { p ->
            if (title.isEmpty()) title = p.title
            if (description.isEmpty()) description = p.description
            status = p.status
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (projectId == null) "New Project" else title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
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
                // Edit form
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Description / Outcome") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3
                    )
                    if (projectId != null) {
                        ExposedDropdownMenuBox(expanded = showStatusMenu, onExpandedChange = { showStatusMenu = it }) {
                            OutlinedTextField(
                                value = status.name.lowercase().replaceFirstChar { it.uppercase() },
                                onValueChange = {}, readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusMenu) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                                ProjectStatus.values().forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        onClick = { status = s; showStatusMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Read-only detail view
                state.project?.let { project ->
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(label = project.status.name.lowercase().replaceFirstChar { it.uppercase() })
                        if (project.description.isNotBlank()) {
                            Text(project.description, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Next Action
                    val nextAction = state.tasks.find { it.id == project.nextActionId }
                    SectionHeader("Next Action")
                    if (nextAction != null) {
                        ListItem(
                            headlineContent = { Text(nextAction.title, fontWeight = FontWeight.SemiBold) },
                            leadingContent = { Icon(Icons.Default.ArrowForward, null) },
                            modifier = Modifier.clickable { onNavigateToTask(nextAction.id) }
                        )
                    } else {
                        Text("No next action set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }

                    // Tasks
                    SectionHeader("Tasks (${state.tasks.size})")
                    state.tasks.forEach { task ->
                        ListItem(
                            headlineContent = { Text(task.title) },
                            leadingContent = {
                                Checkbox(checked = false, onCheckedChange = { viewModel.completeTask(task) })
                            },
                            modifier = Modifier.clickable { onNavigateToTask(task.id) }
                        )
                    }

                    // Waiting
                    if (state.waitingItems.isNotEmpty()) {
                        SectionHeader("Waiting On (${state.waitingItems.size})")
                        state.waitingItems.forEach { item ->
                            ListItem(
                                headlineContent = { Text(item.title) },
                                supportingContent = if (item.person.isNotBlank()) { { Text(item.person) } } else null,
                                leadingContent = { Icon(Icons.Default.HourglassEmpty, null) },
                                modifier = Modifier.clickable { onNavigateToWaiting(item.id) }
                            )
                        }
                    }

                    // Notes
                    if (state.notes.isNotEmpty()) {
                        SectionHeader("Notes (${state.notes.size})")
                        state.notes.forEach { note ->
                            ListItem(
                                headlineContent = { Text(note.title) },
                                leadingContent = { Icon(Icons.Default.Note, null) },
                                modifier = Modifier.clickable { onNavigateToNote(note.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun Chip(label: String) {
    SuggestionChip(onClick = {}, label = { Text(label) })
}
