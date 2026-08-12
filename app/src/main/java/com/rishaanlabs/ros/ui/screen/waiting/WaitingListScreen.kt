package com.rishaanlabs.ros.ui.screen.waiting

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
import com.rishaanlabs.ros.data.local.entity.WaitingItem
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitingListScreen(
    onItemClick: (String) -> Unit,
    onCreateItem: () -> Unit,
    onBack: () -> Unit,
    viewModel: WaitingListViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Waiting") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateItem) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nothing waiting", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { item ->
                    WaitingListItem(item = item, onClick = { onItemClick(item.id) }, onResolve = { viewModel.resolve(item) })
                }
            }
        }
    }
}

@Composable
private fun WaitingListItem(item: WaitingItem, onClick: () -> Unit, onResolve: () -> Unit) {
    val isOverdue = item.followUpDate?.toLocalDate()?.isBefore(LocalDate.now()) == true
    ListItem(
        headlineContent = { Text(item.title) },
        supportingContent = {
            Column {
                if (item.person.isNotBlank()) Text("Waiting on: ${item.person}")
                item.followUpDate?.let {
                    Text(
                        "Follow up: ${it.toLocalDate()}",
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            IconButton(onClick = onResolve) { Icon(Icons.Default.CheckCircle, "Resolve") }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}
