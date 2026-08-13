package com.rishaanlabs.ros.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rishaanlabs.ros.domain.attention.AttentionItem
import com.rishaanlabs.ros.domain.attention.AttentionSeverity

/**
 * Shared building blocks for the V0.1.1 screens.
 *
 * Hierarchy here comes from type, spacing and order rather than colour. Nothing in this file
 * paints a background just to fill space, and only genuinely urgent things are allowed to reach
 * for the error colour.
 */

/** A quiet, all-caps section label. Sections are separated by space, not by boxes. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (trailing != null) {
            if (onTrailingClick != null) {
                TextButton(onClick = onTrailingClick) {
                    Text(trailing, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * An empty state that reads as a normal, calm condition rather than a fault.
 * An empty day is not a problem to be solved.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

/** A small piece of context attached to a card — a due date, a project name, a person. */
@Composable
fun MetadataChip(
    text: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (emphasised) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (emphasised) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

/**
 * One row in a Needs Attention list.
 *
 * Severity is carried by a small leading dot rather than by the whole card, which keeps a screen
 * with several attention items from turning into a wall of warning colour.
 */
@Composable
fun AttentionCard(
    item: AttentionItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val dotColour = when (item.severity) {
        AttentionSeverity.URGENT -> MaterialTheme.colorScheme.error
        AttentionSeverity.NEEDS_ATTENTION -> MaterialTheme.colorScheme.primary
        AttentionSeverity.INFORMATIONAL -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColour, CircleShape)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.severity == AttentionSeverity.URGENT) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * A task the user has chosen as one of today's few priorities.
 *
 * Deliberately larger and more spacious than an ordinary task row — the visual weight is the
 * whole point, since these are the things the day should be protected for.
 */
@Composable
fun PriorityTaskCard(
    title: String,
    modifier: Modifier = Modifier,
    metadata: List<String> = emptyList(),
    overdue: Boolean = false,
    onClick: () -> Unit = {},
    onToggleComplete: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompletionControl(completed = false, onToggle = onToggleComplete)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (metadata.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        metadata.forEachIndexed { index, value ->
                            MetadataChip(text = value, emphasised = overdue && index == 0)
                        }
                    }
                }
            }
        }
    }
}

/** An ordinary task row: the same information, without the visual weight of a priority. */
@Composable
fun TaskRow(
    title: String,
    modifier: Modifier = Modifier,
    completed: Boolean = false,
    metadata: List<String> = emptyList(),
    overdue: Boolean = false,
    onClick: () -> Unit = {},
    onToggleComplete: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompletionControl(completed = completed, onToggle = onToggleComplete)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (metadata.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    metadata.forEachIndexed { index, value ->
                        MetadataChip(text = value, emphasised = overdue && index == 0)
                    }
                }
            }
        }
    }
}

/**
 * The tap target for completing a task. Sized to Material's 48dp minimum so it can be hit
 * comfortably with a thumb without the icon itself being oversized.
 */
@Composable
private fun CompletionControl(
    completed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (completed) "Mark as not done" else "Mark as done",
            tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** A navigation row showing how much sits behind it, used for the project sections. */
@Composable
fun ProjectSummaryRow(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (highlight && count > 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/** Shared formatting for dates shown on cards, so "Today" means the same thing everywhere. */
object Metadata {
    fun dueLabel(due: java.time.LocalDate, today: java.time.LocalDate): String {
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, due)
        return when {
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            days == -1L -> "Yesterday"
            days < 0 -> "${-days} days ago"
            days < 7 -> "In $days days"
            else -> due.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
        }
    }
}
