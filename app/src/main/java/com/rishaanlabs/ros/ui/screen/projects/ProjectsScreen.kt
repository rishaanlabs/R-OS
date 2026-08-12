package com.rishaanlabs.ros.ui.screen.projects

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.data.local.entity.Project

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onProjectClick: (String) -> Unit,
    onCreateProject: () -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Projects") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateProject) {
                Icon(Icons.Default.Add, contentDescription = "Create Project")
            }
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No active projects", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                items(projects, key = { it.id }) { project ->
                    ProjectListItem(project = project, onClick = { onProjectClick(project.id) })
                }
            }
        }
    }
}

@Composable
private fun ProjectListItem(project: Project, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(project.title) },
        supportingContent = if (project.description.isNotBlank()) {
            { Text(project.description, maxLines = 2) }
        } else null,
        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}
