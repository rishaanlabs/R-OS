package com.rishaanlabs.ros.ui.screen.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.data.local.entity.TaskPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String?,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf(state.task?.title ?: "") }
    var description by remember { mutableStateOf(state.task?.description ?: "") }
    var priority by remember { mutableStateOf(state.task?.priority ?: TaskPriority.NONE) }
    var isTopPriority by remember { mutableStateOf(state.task?.isTopPriority ?: false) }
    var isSomeday by remember { mutableStateOf(state.task?.isSomeday ?: false) }
    var selectedProjectId by remember { mutableStateOf(state.task?.projectId) }
    var showProjectMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.task) {
        state.task?.let { t ->
            if (title.isEmpty()) title = t.title
            if (description.isEmpty()) description = t.description
            priority = t.priority
            isTopPriority = t.isTopPriority
            isSomeday = t.isSomeday
            selectedProjectId = t.projectId
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "New Task" else "Edit Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.task != null) {
                        IconButton(onClick = { viewModel.delete() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    TextButton(onClick = {
                        viewModel.save(title, description, priority, selectedProjectId, null, isTopPriority, isSomeday)
                    }) { Text("Save") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Priority selector
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.values().forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Toggles
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilterChip(
                    selected = isTopPriority,
                    onClick = { isTopPriority = !isTopPriority },
                    label = { Text("Top Priority") },
                    leadingIcon = if (isTopPriority) {
                        { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = isSomeday,
                    onClick = { isSomeday = !isSomeday },
                    label = { Text("Someday") }
                )
            }

            // Project assignment
            ExposedDropdownMenuBox(
                expanded = showProjectMenu,
                onExpandedChange = { showProjectMenu = it }
            ) {
                OutlinedTextField(
                    value = projects.find { it.id == selectedProjectId }?.title ?: "No project",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Project") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProjectMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = showProjectMenu, onDismissRequest = { showProjectMenu = false }) {
                    DropdownMenuItem(text = { Text("No project") }, onClick = { selectedProjectId = null; showProjectMenu = false })
                    projects.forEach { project ->
                        DropdownMenuItem(
                            text = { Text(project.title) },
                            onClick = { selectedProjectId = project.id; showProjectMenu = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
