package com.rishaanlabs.ros.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.domain.attention.AttentionEngine
import com.rishaanlabs.ros.ui.components.SectionHeader
import com.rishaanlabs.ros.ui.components.TaskRow

private enum class PlanStep { REVIEW, CHOOSE }

/**
 * Plan My Day.
 *
 * Two steps only: decide what happens to work that did not get done, then choose the few things
 * today is actually for. Deliberately not a planning ritual — no durations, no capacity, no
 * calendar. Those need a much stronger data model than V0.1.1 has, and adding a weak version of
 * them would make the feature feel worse rather than better.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanMyDaySheet(
    onDismiss: () -> Unit,
    viewModel: HomeViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // If there is nothing to review, the review step has no purpose, so skip straight to choosing.
    var step by remember {
        mutableStateOf(if (state.unfinishedCount > 0) PlanStep.REVIEW else PlanStep.CHOOSE)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            when (step) {
                PlanStep.REVIEW -> ReviewStep(
                    tasks = HomeViewModel.unfinishedCandidates(
                        state.topPriorities + state.otherTasks,
                        state.date
                    ),
                    onToday = viewModel::scheduleForToday,
                    onLater = viewModel::scheduleForLater,
                    onSomeday = viewModel::moveToSomeday,
                    onCancel = viewModel::cancelTask,
                    onContinue = { step = PlanStep.CHOOSE }
                )

                PlanStep.CHOOSE -> ChooseStep(
                    candidates = (state.topPriorities + state.otherTasks).distinctBy { it.id },
                    chosen = state.topPriorities.map { it.id }.toSet(),
                    onToggle = viewModel::toggleTopPriority,
                    onFinish = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ReviewStep(
    tasks: List<Task>,
    onToday: (Task) -> Unit,
    onLater: (Task) -> Unit,
    onSomeday: (Task) -> Unit,
    onCancel: (Task) -> Unit,
    onContinue: () -> Unit
) {
    Column {
        Text(
            text = "Unfinished",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
        )
        Text(
            text = "What should happen with these?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        if (tasks.isEmpty()) {
            Text(
                text = "Nothing was left over.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(20.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(tasks, key = { it.id }) { task ->
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            TextButton(onClick = { onToday(task) }) { Text("Today") }
                            TextButton(onClick = { onLater(task) }) { Text("Later") }
                            TextButton(onClick = { onSomeday(task) }) { Text("Someday") }
                            TextButton(onClick = { onCancel(task) }) { Text("Cancel") }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onContinue) { Text("Continue") }
        }
    }
}

@Composable
private fun ChooseStep(
    candidates: List<Task>,
    chosen: Set<String>,
    onToggle: (Task) -> Unit,
    onFinish: () -> Unit
) {
    Column {
        Text(
            text = "Today's priorities",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
        )
        Text(
            text = "If the day falls apart, these are what you still want done. " +
                "Up to ${AttentionEngine.MAX_TOP_PRIORITIES}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        SectionHeader(
            title = "Chosen",
            trailing = "${chosen.size} of ${AttentionEngine.MAX_TOP_PRIORITIES}"
        )

        if (candidates.isEmpty()) {
            Text(
                text = "There are no open tasks to choose from yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(candidates, key = { it.id }) { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TaskRow(
                            title = task.title,
                            completed = task.id in chosen,
                            metadata = if (task.id in chosen) listOf("Priority") else emptyList(),
                            onClick = { onToggle(task) },
                            onToggleComplete = { onToggle(task) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onFinish) { Text("Done") }
        }
    }
}
