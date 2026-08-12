package com.rishaanlabs.ros.ui.screen.search

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNavigateToTask: (String) -> Unit,
    onNavigateToNote: (String) -> Unit,
    onNavigateToProject: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    val hasResults = results.tasks.isNotEmpty() || results.notes.isNotEmpty() || results.waiting.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.setQuery(it) },
                        placeholder = { Text("Search…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            { IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Default.Clear, "Clear") } }
                        } else null
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (query.length < 2) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Type at least 2 characters to search", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (!hasResults) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No results for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                if (results.tasks.isNotEmpty()) {
                    item { SectionHeader("Tasks") }
                    items(results.tasks) { task ->
                        ListItem(
                            headlineContent = { Text(task.title) },
                            leadingContent = { Icon(Icons.Default.CheckCircle, null) },
                            modifier = Modifier.clickable { onNavigateToTask(task.id) }
                        )
                    }
                }
                if (results.notes.isNotEmpty()) {
                    item { SectionHeader("Notes") }
                    items(results.notes) { note ->
                        ListItem(
                            headlineContent = { Text(note.title) },
                            leadingContent = { Icon(Icons.Default.Note, null) },
                            modifier = Modifier.clickable { onNavigateToNote(note.id) }
                        )
                    }
                }
                if (results.waiting.isNotEmpty()) {
                    item { SectionHeader("Waiting") }
                    items(results.waiting) { item ->
                        ListItem(
                            headlineContent = { Text(item.title) },
                            supportingContent = if (item.person.isNotBlank()) { { Text(item.person) } } else null,
                            leadingContent = { Icon(Icons.Default.HourglassEmpty, null) }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
