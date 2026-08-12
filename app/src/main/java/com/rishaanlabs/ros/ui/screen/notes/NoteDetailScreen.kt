package com.rishaanlabs.ros.ui.screen.notes

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String?,
    onBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var showProjectMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.note) {
        state.note?.let { n ->
            if (title.isEmpty()) title = n.title
            if (body.isEmpty()) body = n.body
            selectedProjectId = n.projectId
        }
    }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "New Note" else "Note") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (state.note != null) {
                        IconButton(onClick = { viewModel.delete() }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                    TextButton(onClick = { viewModel.save(title, body, selectedProjectId) }) { Text("Save") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it },
                label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = body, onValueChange = { body = it },
                label = { Text("Content (Markdown supported)") },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp),
                minLines = 8)

            ExposedDropdownMenuBox(expanded = showProjectMenu, onExpandedChange = { showProjectMenu = it }) {
                OutlinedTextField(
                    value = projects.find { it.id == selectedProjectId }?.title ?: "No project",
                    onValueChange = {}, readOnly = true,
                    label = { Text("Project") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProjectMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = showProjectMenu, onDismissRequest = { showProjectMenu = false }) {
                    DropdownMenuItem(text = { Text("No project") }, onClick = { selectedProjectId = null; showProjectMenu = false })
                    projects.forEach { p ->
                        DropdownMenuItem(text = { Text(p.title) }, onClick = { selectedProjectId = p.id; showProjectMenu = false })
                    }
                }
            }
        }
    }
}
