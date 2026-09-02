package com.rishaanlabs.ros.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.domain.attention.AttentionItem
import com.rishaanlabs.ros.domain.attention.AttentionSeverity
import com.rishaanlabs.ros.domain.attention.AttentionTarget
import com.rishaanlabs.ros.ui.components.RosCard
import com.rishaanlabs.ros.ui.components.RosDotRow
import com.rishaanlabs.ros.ui.components.RosHairline
import com.rishaanlabs.ros.ui.components.RosIndex
import com.rishaanlabs.ros.ui.components.RosMeter
import com.rishaanlabs.ros.ui.components.RosSectionRule
import com.rishaanlabs.ros.ui.theme.Ros
import com.rishaanlabs.ros.ui.theme.RosDisplayNumeric
import com.rishaanlabs.ros.ui.theme.RosLabel
import com.rishaanlabs.ros.ui.theme.RosMeta
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home, as a briefing.
 *
 * The order is an argument about what matters, and it is the same argument V0.1.1 made: today's
 * chosen few, then the money, then what has gone wrong without you, then who owes you something.
 * What changed is the language it is written in — sections announced by a monospaced rule rather
 * than by cards, figures set in mono so they line up and can be compared, and colour reserved for
 * the one dot that says a thing is late.
 *
 * There is no "upcoming payments" block, which the design also shows. Scheduled payments have no
 * table; inventing one is a migration, and the roadmap holds those for V0.2B.
 */
@Composable
fun HomeScreen(
    onNavigateToInbox: () -> Unit,
    onNavigateToTask: (String) -> Unit,
    onNavigateToWaiting: () -> Unit,
    onNavigateToProject: (String) -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onCapture: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var planningDay by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item { DayHeader(state, onFocus = { planningDay = true }) }

        item {
            RosSectionRule(
                label = "Top 3 · chosen",
                trailing = "${state.topPriorities.size}/3",
                modifier = Modifier.padding(top = 18.dp)
            )
        }

        item {
            RosCard(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
                if (state.topPriorities.isEmpty()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 18.dp)) {
                        Text(
                            "Nothing chosen yet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = Ros.colors.ink
                        )
                        Text(
                            "Zero is a valid answer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Ros.colors.ink3,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                } else {
                    state.topPriorities.forEachIndexed { index, task ->
                        PriorityRow(
                            index = index + 1,
                            task = task,
                            onOpen = { onNavigateToTask(task.id) },
                            onToggle = { viewModel.completeTask(task) }
                        )
                        if (index != state.topPriorities.lastIndex) RosHairline()
                    }
                }
            }
        }

        if (state.finance.hasAccounts) {
            item {
                RosSectionRule(
                    label = "Finance",
                    trailing = "Open",
                    onTrailingClick = onNavigateToFinance,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            item { FinanceLine(state.finance) }
        }

        if (state.attention.isNotEmpty()) {
            item {
                RosSectionRule(
                    label = "Needs attention",
                    trailing = state.attention.size.toString(),
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
                    RosHairline()
                    state.attention.forEach { item ->
                        RosDotRow(
                            text = item.title,
                            dotColor = item.dotColor(),
                            trailing = item.trailing(),
                            onClick = { item.navigate(onNavigateToTask, onNavigateToProject, onNavigateToWaiting, onNavigateToInbox) }
                        )
                        RosHairline()
                    }
                }
            }
        }

        if (state.inboxCount > 0) {
            item {
                RosSectionRule(label = "Inbox", modifier = Modifier.padding(top = 20.dp))
            }
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
                    RosHairline()
                    RosDotRow(
                        text = if (state.inboxCount == 1) {
                            "1 thing unprocessed"
                        } else {
                            "${state.inboxCount} things unprocessed"
                        },
                        dotColor = Ros.colors.ink4,
                        trailing = "PROCESS",
                        onClick = onNavigateToInbox
                    )
                    RosHairline()
                }
            }
        }

        if (state.waiting.isNotEmpty()) {
            item {
                RosSectionRule(label = "Waiting on", modifier = Modifier.padding(top = 20.dp))
            }
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
                    RosHairline()
                    state.waiting.forEach { summary ->
                        WaitingRow(summary, onClick = onNavigateToWaiting)
                        RosHairline()
                    }
                }
            }
        }

        // Clears the capture bar and the bottom navigation, which float over this list.
        item { Box(Modifier.height(120.dp)) }
    }

    if (planningDay) {
        PlanMyDaySheet(
            onDismiss = { planningDay = false },
            viewModel = viewModel
        )
    }
}

@Composable
private fun DayHeader(state: HomeUiState, onFocus: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = state.date.format(DateTimeFormatter.ofPattern("EEE d MMM")).uppercase(),
                style = RosLabel,
                color = Ros.colors.ink3
            )
            Text(
                text = "Your day",
                style = MaterialTheme.typography.displaySmall,
                color = Ros.colors.ink,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Text(
            text = "FOCUS",
            style = RosLabel,
            color = Ros.colors.ink2,
            modifier = Modifier
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Ros.colors.line, RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onFocus)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun PriorityRow(
    index: Int,
    task: Task,
    onOpen: () -> Unit,
    onToggle: () -> Unit
) {
    val done = task.completedAt != null
    Row(
        Modifier.fillMaxWidth().padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        RosIndex(index, Modifier.padding(top = 2.dp))
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (done) Ros.colors.ink3 else Ros.colors.ink,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = task.homeMeta()
            Text(
                text = meta.text,
                style = RosMeta,
                color = if (meta.overdue) Ros.colors.danger else Ros.colors.ink3,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        // A checkbox, stated as one: V0.1.1 found that reusing a row control whose meaning was
        // "open" for something that completes a task misdescribes it to a screen reader.
        Box(
            Modifier
                .padding(top = 1.dp)
                .size(19.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (done) Ros.colors.acc else Color.Transparent)
                .border(1.6.dp, Ros.colors.ink4, RoundedCornerShape(4.dp))
                .clickable(role = Role.Checkbox, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.labelLarge,
                    color = Ros.colors.onAcc
                )
            }
        }
    }
}

@Composable
private fun FinanceLine(finance: HomeFinance) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                finance.currency,
                style = RosMeta.copy(fontSize = 12.sp),
                color = Ros.colors.ink3,
                modifier = Modifier.padding(end = 8.dp, bottom = 3.dp)
            )
            Text(
                text = finance.totalCashMinor.plain(),
                style = RosDisplayNumeric,
                color = Ros.colors.ink
            )
            Text(
                text = "${finance.freeMinor.plain()} free",
                style = MaterialTheme.typography.bodySmall,
                color = Ros.colors.ink3,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, bottom = 3.dp),
                textAlign = TextAlign.End
            )
        }

        val percent = finance.planPercent
        if (percent != null) {
            RosMeter(
                fraction = percent / 100f,
                fillColor = if (percent > 100) Ros.colors.danger else Ros.colors.acc,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${finance.spentAgainstPlanMinor.plain()} of ${finance.plannedMinor.plain()} plan",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ros.colors.ink3
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (percent > 100) Ros.colors.danger else Ros.colors.ink3
                )
            }
        } else {
            Text(
                text = "No monthly limits set, so there is nothing to measure this against.",
                style = MaterialTheme.typography.bodySmall,
                color = Ros.colors.ink3,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun WaitingRow(summary: WaitingSummary, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = summary.item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Ros.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val who = summary.item.person.ifBlank { "someone" }.uppercase()
            Text(
                text = "ON $who · SINCE " +
                    summary.item.requestedDate.format(DateTimeFormatter.ofPattern("d MMM")).uppercase(),
                style = RosMeta,
                color = Ros.colors.ink3,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Text(
            text = "${summary.daysWaiting}d",
            style = MaterialTheme.typography.bodySmall,
            color = if (summary.isStale) Ros.colors.danger else Ros.colors.ink3
        )
    }
}

private data class HomeMeta(val text: String, val overdue: Boolean)

/** The mono line under a priority: project and date, upper case, or an explicit "no date". */
@Composable
private fun Task.homeMeta(): HomeMeta {
    val overdue = dueDate != null &&
        dueDate.toLocalDate().isBefore(LocalDate.now()) &&
        completedAt == null
    val due = when {
        dueDate == null -> null
        overdue -> "OVERDUE"
        dueDate.toLocalDate() == LocalDate.now() -> "TODAY"
        else -> dueDate.format(DateTimeFormatter.ofPattern("d MMM")).uppercase()
    }
    val text = due ?: "NO DATE"
    return HomeMeta(text, overdue)
}

@Composable
private fun AttentionItem.dotColor() = when (severity) {
    AttentionSeverity.URGENT -> Ros.colors.danger
    AttentionSeverity.NEEDS_ATTENTION -> Ros.colors.warn
    AttentionSeverity.INFORMATIONAL -> Ros.colors.ink4
}

private fun AttentionItem.trailing(): String = when (target) {
    AttentionTarget.INBOX -> "INBOX"
    AttentionTarget.WAITING -> "WAITING"
    AttentionTarget.PROJECT -> "PROJECT"
    else -> ""
}

private fun AttentionItem.navigate(
    onTask: (String) -> Unit,
    onProject: (String) -> Unit,
    onWaiting: () -> Unit,
    onInbox: () -> Unit
) {
    when (target) {
        AttentionTarget.TASK -> targetId?.let(onTask)
        AttentionTarget.PROJECT -> targetId?.let(onProject)
        AttentionTarget.WAITING -> onWaiting()
        AttentionTarget.INBOX -> onInbox()
        AttentionTarget.NONE -> Unit
    }
}

/** Money without its currency code, for lines that already state the currency once. */
private fun Long.plain(): String =
    BigDecimal.valueOf(this, 2).let { amount ->
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(amount)
    }
