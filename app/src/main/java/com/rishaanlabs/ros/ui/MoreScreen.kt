package com.rishaanlabs.ros.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Folder
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
    onNavigateToProjects: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToWaiting: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToDailyReview: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Everything else") }) }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            // Projects lives here since the bottom bar went to four tabs. It is first because
            // it is the only one of these that used to have a tab of its own.
            item { MoreItem(Icons.Default.Folder, "Projects", onNavigateToProjects) }
            item { MoreItem(Icons.Default.Inbox, "Inbox", onNavigateToInbox) }
            item { MoreItem(Icons.Default.Note, "Notes", onNavigateToNotes) }
            item { MoreItem(Icons.Default.HourglassEmpty, "Waiting", onNavigateToWaiting) }
            item { MoreItem(Icons.Default.AccountBalanceWallet, "Finance", onNavigateToFinance) }
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
