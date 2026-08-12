package com.rishaanlabs.ros.ui.screen.waiting

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
fun WaitingDetailScreen(
    waitingId: String?,
    onBack: () -> Unit,
    viewModel: WaitingDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var person by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var showProjectMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.item) {
        state.item?.let { i ->
            if (title.isEmpty()) title = i.title
            if (person.isEmpty()) person = i.person
            if (description.isEmpty()) description = i.description
            selectedProjectId = i.projectId
        }
    }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (waitingId == null) "New Waiting Item" else "Waiting Item") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (state.item != null) {
                        TextButton(onClick = { viewModel.resolve() }) { Text("Resolved") }
                        IconButton(onClick = { viewModel.delete() }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                    TextButton(onClick = { viewModel.save(title, person, description, selectedProjectId, null) }) { Text("Save") }
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
                label = { Text("What are you waiting for?") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = person, onValueChange = { person = it },
                label = { Text("Waiting on (person/org)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

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
