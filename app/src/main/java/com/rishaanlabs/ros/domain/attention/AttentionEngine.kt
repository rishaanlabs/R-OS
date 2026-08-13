package com.rishaanlabs.ros.domain.attention

import com.rishaanlabs.ros.data.local.entity.Project
import com.rishaanlabs.ros.data.local.entity.ProjectStatus
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.TaskPriority
import com.rishaanlabs.ros.data.local.entity.TaskStatus
import com.rishaanlabs.ros.data.local.entity.WaitingItem
import com.rishaanlabs.ros.data.local.entity.WaitingStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Turns the raw contents of the database into a short list of things worth looking at.
 *
 * This is deliberately rule-based and deliberately pure: it takes entities in and returns
 * [AttentionItem]s out, with the current date passed in rather than read from the clock. That
 * makes every rule directly unit-testable, and it keeps the interpretation out of both the
 * Composables and the database.
 *
 * It is not, and should not become, a prediction engine. Anything smarter belongs in a later
 * version; this layer exists so the app can be useful about state it can already see.
 */
object AttentionEngine {

    /** Beyond this, an overdue item stops being a nudge and starts being a problem. */
    private const val URGENT_OVERDUE_DAYS = 3L

    /** An active project untouched for this long is probably drifting. */
    private const val INACTIVE_PROJECT_DAYS = 14L

    /** More chosen priorities than this and the point of choosing them is lost. */
    const val MAX_TOP_PRIORITIES = 3

    /**
     * Evaluates the whole system.
     *
     * @param tasks every task; completed and cancelled ones are filtered out here rather than
     *   by the caller, so a caller cannot accidentally raise attention for finished work.
     */
    fun evaluate(
        tasks: List<Task>,
        projects: List<Project>,
        waiting: List<WaitingItem>,
        inboxUnprocessedCount: Int,
        today: LocalDate
    ): List<AttentionItem> {
        val items = mutableListOf<AttentionItem>()

        items += waiting.mapNotNull { overdueWaiting(it, today) }
        items += tasks.mapNotNull { overdueTask(it, today) }
        items += projects.mapNotNull { project -> projectWithoutNextAction(project, tasks) }
        items += projects.mapNotNull { project -> inactiveProject(project, tasks, today) }
        inboxBacklog(inboxUnprocessedCount)?.let { items += it }
        tooManyPriorities(tasks)?.let { items += it }

        return items.sortedWith(compareByDescending<AttentionItem> { it.severity.ordinal }.thenBy { it.title })
    }

    /**
     * Evaluates a single project, for the project's own page. Callers pass only that project's
     * related records.
     */
    fun evaluateForProject(
        project: Project,
        projectTasks: List<Task>,
        projectWaiting: List<WaitingItem>,
        today: LocalDate
    ): List<AttentionItem> {
        val items = mutableListOf<AttentionItem>()

        items += projectWaiting.mapNotNull { overdueWaiting(it, today) }
        items += projectTasks.mapNotNull { overdueTask(it, today) }
        projectWithoutNextAction(project, projectTasks)?.let { items += it }

        return items.sortedWith(compareByDescending<AttentionItem> { it.severity.ordinal }.thenBy { it.title })
    }

    // ---------------------------------------------------------------- rules

    /**
     * Someone else owes us something and the date we said we would chase it has passed.
     * Resolved and cancelled items are never overdue — the waiting is over.
     */
    private fun overdueWaiting(item: WaitingItem, today: LocalDate): AttentionItem? {
        if (item.status != WaitingStatus.WAITING && item.status != WaitingStatus.FOLLOW_UP_DUE) return null
        val followUp = item.followUpDate?.toLocalDate() ?: return null
        if (!followUp.isBefore(today)) return null

        val days = ChronoUnit.DAYS.between(followUp, today)
        return AttentionItem(
            id = "waiting-overdue-${item.id}",
            kind = AttentionKind.WAITING_FOLLOW_UP_OVERDUE,
            severity = if (days >= URGENT_OVERDUE_DAYS) AttentionSeverity.URGENT else AttentionSeverity.NEEDS_ATTENTION,
            title = item.title,
            detail = buildString {
                append("Follow-up ")
                append(days)
                append(if (days == 1L) " day overdue" else " days overdue")
                if (item.person.isNotBlank()) append(" · ${item.person}")
            },
            target = AttentionTarget.WAITING,
            targetId = item.id
        )
    }

    /** An open task whose due date has passed. Completed work is never overdue. */
    private fun overdueTask(task: Task, today: LocalDate): AttentionItem? {
        if (task.status != TaskStatus.OPEN) return null
        val due = task.dueDate?.toLocalDate() ?: return null
        if (!due.isBefore(today)) return null

        val days = ChronoUnit.DAYS.between(due, today)
        val urgent = task.priority == TaskPriority.HIGH || task.isTopPriority || days >= URGENT_OVERDUE_DAYS
        return AttentionItem(
            id = "task-overdue-${task.id}",
            kind = AttentionKind.OVERDUE_TASK,
            severity = if (urgent) AttentionSeverity.URGENT else AttentionSeverity.NEEDS_ATTENTION,
            title = task.title,
            detail = if (days == 1L) "1 day overdue" else "$days days overdue",
            target = AttentionTarget.TASK,
            targetId = task.id
        )
    }

    /**
     * An active project with work left but nothing marked as the thing to do next.
     *
     * A next action that points at a task which has since been completed or deleted counts as
     * missing — a stale pointer is worse than an empty one, because it silently stops being true.
     */
    private fun projectWithoutNextAction(project: Project, tasks: List<Task>): AttentionItem? {
        if (project.status != ProjectStatus.ACTIVE) return null

        val openTasks = tasks.filter { it.projectId == project.id && it.status == TaskStatus.OPEN }
        if (openTasks.isEmpty()) return null

        val nextAction = project.nextActionId?.let { id -> openTasks.firstOrNull { it.id == id } }
        if (nextAction != null) return null

        return AttentionItem(
            id = "project-no-next-action-${project.id}",
            kind = AttentionKind.PROJECT_NO_NEXT_ACTION,
            severity = AttentionSeverity.NEEDS_ATTENTION,
            title = project.title,
            detail = "No next action",
            target = AttentionTarget.PROJECT,
            targetId = project.id
        )
    }

    /**
     * An active project with open work that has not been touched in a while. Informational only —
     * plenty of projects legitimately sit still.
     */
    private fun inactiveProject(project: Project, tasks: List<Task>, today: LocalDate): AttentionItem? {
        if (project.status != ProjectStatus.ACTIVE) return null

        val openTasks = tasks.filter { it.projectId == project.id && it.status == TaskStatus.OPEN }
        if (openTasks.isEmpty()) return null

        val days = ChronoUnit.DAYS.between(project.updatedAt.toLocalDate(), today)
        if (days < INACTIVE_PROJECT_DAYS) return null

        return AttentionItem(
            id = "project-inactive-${project.id}",
            kind = AttentionKind.PROJECT_INACTIVE,
            severity = AttentionSeverity.INFORMATIONAL,
            title = project.title,
            detail = "No activity for $days days",
            target = AttentionTarget.PROJECT,
            targetId = project.id
        )
    }

    /** Captured thoughts that have not been given a home yet. */
    private fun inboxBacklog(count: Int): AttentionItem? {
        if (count <= 0) return null
        return AttentionItem(
            id = "inbox-unprocessed",
            kind = AttentionKind.INBOX_UNPROCESSED,
            severity = AttentionSeverity.INFORMATIONAL,
            title = "Inbox",
            detail = if (count == 1) "1 thing needs processing" else "$count things need processing",
            target = AttentionTarget.INBOX
        )
    }

    /** Choosing everything is the same as choosing nothing. */
    private fun tooManyPriorities(tasks: List<Task>): AttentionItem? {
        val chosen = tasks.count { it.status == TaskStatus.OPEN && it.isTopPriority }
        if (chosen <= MAX_TOP_PRIORITIES) return null
        return AttentionItem(
            id = "too-many-priorities",
            kind = AttentionKind.TOO_MANY_PRIORITIES,
            severity = AttentionSeverity.INFORMATIONAL,
            title = "$chosen priorities chosen for today",
            detail = "Keeping it to $MAX_TOP_PRIORITIES makes the day easier to protect",
            target = AttentionTarget.NONE
        )
    }
}
