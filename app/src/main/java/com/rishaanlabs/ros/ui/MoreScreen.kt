package com.rishaanlabs.ros.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToInbox: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToWaiting: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToDailyReview: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("More") }) }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { MoreItem(Icons.Default.Inbox, "Inbox", onNavigateToInbox) }
            item { MoreItem(Icons.Default.Note, "Notes", onNavigateToNotes) }
            item { MoreItem(Icons.Default.HourglassEmpty, "Waiting", onNavigateToWaiting) }
            item { MoreItem(Icons.Default.Search, "Search", onNavigateToSearch) }
            item { MoreItem(Icons.Default.WbSunny, "Daily Review", onNavigateToDailyReview) }
        }
    }
}

@Composable
private fun MoreItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}
