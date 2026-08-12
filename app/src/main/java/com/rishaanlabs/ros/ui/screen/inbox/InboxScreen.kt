package com.rishaanlabs.ros.ui.screen.inbox

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.data.local.entity.InboxItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var processingItem by remember { mutableStateOf<InboxItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbox") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Inbox is clear",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { item ->
                    InboxItemCard(
                        item = item,
                        onProcess = { processingItem = item },
                        onDelete = { viewModel.delete(item) }
                    )
                }
            }
        }
    }

    processingItem?.let { item ->
        ProcessingSheet(
            item = item,
            onDismiss = { processingItem = null },
            onConvertToTask = { viewModel.convertToTask(item); processingItem = null },
            onConvertToNote = { viewModel.convertToNote(item); processingItem = null },
            onConvertToWaiting = { viewModel.convertToWaiting(item); processingItem = null },
            onDelete = { viewModel.delete(item); processingItem = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxItemCard(
    item: InboxItem,
    onProcess: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onProcess
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.text, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                TextButton(onClick = onProcess) { Text("Process") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessingSheet(
    item: InboxItem,
    onDismiss: () -> Unit,
    onConvertToTask: () -> Unit,
    onConvertToNote: () -> Unit,
    onConvertToWaiting: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Process Item", style = MaterialTheme.typography.titleMedium)
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider()
            ProcessAction(icon = Icons.Default.CheckCircle, label = "Convert to Task", onClick = onConvertToTask)
            ProcessAction(icon = Icons.Default.Note, label = "Convert to Note", onClick = onConvertToNote)
            ProcessAction(icon = Icons.Default.HourglassEmpty, label = "Convert to Waiting", onClick = onConvertToWaiting)
            ProcessAction(icon = Icons.Default.Delete, label = "Delete", onClick = onDelete)
        }
    }
}

@Composable
private fun ProcessAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
