package com.rishaanlabs.ros.ui.screen.daily

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReviewScreen(
    onBack: () -> Unit,
    onHistory: () -> Unit,
    viewModel: DailyReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var energy by remember { mutableIntStateOf(0) }
    var mainFocus by remember { mutableStateOf("") }
    var biggestWin by remember { mutableStateOf("") }
    var unfinished by remember { mutableStateOf("") }
    var toRemember by remember { mutableStateOf("") }
    var tomorrowPriority by remember { mutableStateOf("") }

    LaunchedEffect(state.review) {
        state.review?.let { r ->
            energy = r.energy ?: 0
            mainFocus = r.mainFocus
            biggestWin = r.biggestWin
            unfinished = r.unfinished
            toRemember = r.toRemember
            tomorrowPriority = r.tomorrowPriority
        }
    }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Review") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onHistory) { Icon(Icons.Default.History, "History") }
                    TextButton(onClick = {
                        viewModel.save(
                            energy.takeIf { it > 0 },
                            mainFocus, biggestWin, unfinished, toRemember, tomorrowPriority
                        )
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

            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text("Energy Level", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { level ->
                    FilterChip(
                        selected = energy == level,
                        onClick = { energy = if (energy == level) 0 else level },
                        label = { Text("$level") }
                    )
                }
            }

            ReviewField(value = mainFocus, onValueChange = { mainFocus = it }, label = "Main focus today")
            ReviewField(value = biggestWin, onValueChange = { biggestWin = it }, label = "Biggest win")
            ReviewField(value = unfinished, onValueChange = { unfinished = it }, label = "Left unfinished")
            ReviewField(value = toRemember, onValueChange = { toRemember = it }, label = "Something to remember")
            ReviewField(value = tomorrowPriority, onValueChange = { tomorrowPriority = it }, label = "Tomorrow's priority")

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReviewField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReviewHistoryScreen(
    onBack: () -> Unit,
    viewModel: DailyReviewViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review History") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(history) { review ->
                ListItem(
                    headlineContent = {
                        Text(review.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")))
                    },
                    supportingContent = {
                        Column {
                            review.energy?.let { Text("Energy: $it/5") }
                            if (review.mainFocus.isNotBlank()) Text("Focus: ${review.mainFocus}")
                        }
                    },
                    leadingContent = { Icon(Icons.Default.WbSunny, null) }
                )
                HorizontalDivider()
            }
        }
    }
}
