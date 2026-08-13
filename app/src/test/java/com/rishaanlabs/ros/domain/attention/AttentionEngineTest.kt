package com.rishaanlabs.ros.domain.attention

import com.rishaanlabs.ros.data.local.entity.Project
import com.rishaanlabs.ros.data.local.entity.ProjectStatus
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.TaskPriority
import com.rishaanlabs.ros.data.local.entity.TaskStatus
import com.rishaanlabs.ros.data.local.entity.WaitingItem
import com.rishaanlabs.ros.data.local.entity.WaitingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The attention rules are the one piece of V0.1.1 that interprets the user's data rather than
 * displaying it, so they are the piece most worth pinning down. Every test fixes "today" instead
 * of reading the clock, so none of these can start failing overnight.
 */
class AttentionEngineTest {

    private val today: LocalDate = LocalDate.of(2026, 8, 13)

    private fun task(
        id: String = "task-1",
        title: String = "A task",
        status: TaskStatus = TaskStatus.OPEN,
        priority: TaskPriority = TaskPriority.NONE,
        projectId: String? = null,
        dueDate: LocalDateTime? = null,
        isTopPriority: Boolean = false
    ) = Task(
        id = id,
        title = title,
        status = status,
        priority = priority,
        projectId = projectId,
        dueDate = dueDate,
        isTopPriority = isTopPriority
    )

    private fun project(
        id: String = "project-1",
        title: String = "A project",
        status: ProjectStatus = ProjectStatus.ACTIVE,
        nextActionId: String? = null,
        updatedAt: LocalDateTime = today.atStartOfDay()
    ) = Project(id = id, title = title, status = status, nextActionId = nextActionId, updatedAt = updatedAt)

    private fun waiting(
        id: String = "waiting-1",
        title: String = "A waiting item",
        status: WaitingStatus = WaitingStatus.WAITING,
        followUpDate: LocalDateTime? = null,
        projectId: String? = null
    ) = WaitingItem(id = id, title = title, status = status, followUpDate = followUpDate, projectId = projectId)

    private fun evaluate(
        tasks: List<Task> = emptyList(),
        projects: List<Project> = emptyList(),
        waitingItems: List<WaitingItem> = emptyList(),
        inboxCount: Int = 0
    ) = AttentionEngine.evaluate(tasks, projects, waitingItems, inboxCount, today)

    private fun List<AttentionItem>.ofKind(kind: AttentionKind) = firstOrNull { it.kind == kind }

    // ------------------------------------------------------------- waiting

    @Test
    fun `unresolved waiting item past its follow-up date needs attention`() {
        val items = evaluate(
            waitingItems = listOf(waiting(followUpDate = today.minusDays(4).atStartOfDay()))
        )

        val item = items.ofKind(AttentionKind.WAITING_FOLLOW_UP_OVERDUE)
        assertNotNull("an overdue follow-up should surface", item)
        assertEquals("Follow-up 4 days overdue", item!!.detail)
        assertEquals(AttentionTarget.WAITING, item.target)
    }

    @Test
    fun `resolved waiting item never generates attention`() {
        val items = evaluate(
            waitingItems = listOf(
                waiting(status = WaitingStatus.RESOLVED, followUpDate = today.minusDays(9).atStartOfDay())
            )
        )

        assertNull(items.ofKind(AttentionKind.WAITING_FOLLOW_UP_OVERDUE))
    }

    @Test
    fun `cancelled waiting item never generates attention`() {
        val items = evaluate(
            waitingItems = listOf(
                waiting(status = WaitingStatus.CANCELLED, followUpDate = today.minusDays(9).atStartOfDay())
            )
        )

        assertNull(items.ofKind(AttentionKind.WAITING_FOLLOW_UP_OVERDUE))
    }

    @Test
    fun `waiting item with a future follow-up date is not overdue`() {
        val items = evaluate(waitingItems = listOf(waiting(followUpDate = today.plusDays(2).atStartOfDay())))

        assertNull(items.ofKind(AttentionKind.WAITING_FOLLOW_UP_OVERDUE))
    }

    @Test
    fun `waiting item due today is not yet overdue`() {
        val items = evaluate(waitingItems = listOf(waiting(followUpDate = today.atTime(9, 0))))

        assertNull(items.ofKind(AttentionKind.WAITING_FOLLOW_UP_OVERDUE))
    }

    @Test
    fun `waiting item with no follow-up date is not overdue`() {
        val items = evaluate(waitingItems = listOf(waiting(followUpDate = null)))

        assertNull(items.ofKind(AttentionKind.WAITING_FOLLOW_UP_OVERDUE))
    }

    @Test
    fun `a follow-up only just overdue stays calm rather than urgent`() {
        val items = evaluate(waitingItems = listOf(waiting(followUpDate = today.minusDays(1).atStartOfDay())))

        assertEquals(
            AttentionSeverity.NEEDS_ATTENTION,
            items.ofKind(AttentionKind.WAITING_FOLLOW_UP_OVERDUE)!!.severity
        )
    }

    @Test
    fun `a long overdue follow-up becomes urgent`() {
        val items = evaluate(waitingItems = listOf(waiting(followUpDate = today.minusDays(5).atStartOfDay())))

        assertEquals(
            AttentionSeverity.URGENT,
            items.ofKind(AttentionKind.WAITING_FOLLOW_UP_OVERDUE)!!.severity
        )
    }

    // ------------------------------------------------------- project next action

    @Test
    fun `active project with open tasks but no next action needs attention`() {
        val items = evaluate(
            tasks = listOf(task(projectId = "project-1")),
            projects = listOf(project(nextActionId = null))
        )

        val item = items.ofKind(AttentionKind.PROJECT_NO_NEXT_ACTION)
        assertNotNull(item)
        assertEquals("No next action", item!!.detail)
    }

    @Test
    fun `project with a valid next action does not generate that warning`() {
        val items = evaluate(
            tasks = listOf(task(id = "task-1", projectId = "project-1")),
            projects = listOf(project(nextActionId = "task-1"))
        )

        assertNull(items.ofKind(AttentionKind.PROJECT_NO_NEXT_ACTION))
    }

    @Test
    fun `a next action pointing at a completed task counts as missing`() {
        val items = evaluate(
            tasks = listOf(
                task(id = "task-done", projectId = "project-1", status = TaskStatus.COMPLETED),
                task(id = "task-open", projectId = "project-1")
            ),
            projects = listOf(project(nextActionId = "task-done"))
        )

        assertNotNull(
            "a stale next action is worse than none, so it must still surface",
            items.ofKind(AttentionKind.PROJECT_NO_NEXT_ACTION)
        )
    }

    @Test
    fun `a next action pointing at a deleted task counts as missing`() {
        val items = evaluate(
            tasks = listOf(task(id = "task-open", projectId = "project-1")),
            projects = listOf(project(nextActionId = "task-that-no-longer-exists"))
        )

        assertNotNull(items.ofKind(AttentionKind.PROJECT_NO_NEXT_ACTION))
    }

    @Test
    fun `completed project does not generate a missing next action warning`() {
        val items = evaluate(
            tasks = listOf(task(projectId = "project-1")),
            projects = listOf(project(status = ProjectStatus.COMPLETED))
        )

        assertNull(items.ofKind(AttentionKind.PROJECT_NO_NEXT_ACTION))
    }

    @Test
    fun `paused and archived projects do not generate a missing next action warning`() {
        listOf(ProjectStatus.PAUSED, ProjectStatus.ARCHIVED).forEach { status ->
            val items = evaluate(
                tasks = listOf(task(projectId = "project-1")),
                projects = listOf(project(status = status))
            )
            assertNull("$status should stay quiet", items.ofKind(AttentionKind.PROJECT_NO_NEXT_ACTION))
        }
    }

    @Test
    fun `project with no open tasks does not need a next action`() {
        val items = evaluate(
            tasks = listOf(task(projectId = "project-1", status = TaskStatus.COMPLETED)),
            projects = listOf(project())
        )

        assertNull(items.ofKind(AttentionKind.PROJECT_NO_NEXT_ACTION))
    }

    @Test
    fun `tasks belonging to another project do not satisfy this project's next action`() {
        val items = evaluate(
            tasks = listOf(task(id = "task-1", projectId = "project-2")),
            projects = listOf(project(id = "project-1", nextActionId = "task-1"))
        )

        assertNull(
            "project-1 has no open tasks of its own, so it should not be flagged",
            items.ofKind(AttentionKind.PROJECT_NO_NEXT_ACTION)
        )
    }

    // -------------------------------------------------------------- tasks

    @Test
    fun `overdue open task generates attention`() {
        val items = evaluate(tasks = listOf(task(dueDate = today.minusDays(1).atStartOfDay())))

        val item = items.ofKind(AttentionKind.OVERDUE_TASK)
        assertNotNull(item)
        assertEquals("1 day overdue", item!!.detail)
    }

    @Test
    fun `completed overdue task does not generate attention`() {
        val items = evaluate(
            tasks = listOf(
                task(status = TaskStatus.COMPLETED, dueDate = today.minusDays(6).atStartOfDay())
            )
        )

        assertNull(items.ofKind(AttentionKind.OVERDUE_TASK))
    }

    @Test
    fun `cancelled overdue task does not generate attention`() {
        val items = evaluate(
            tasks = listOf(
                task(status = TaskStatus.CANCELLED, dueDate = today.minusDays(6).atStartOfDay())
            )
        )

        assertNull(items.ofKind(AttentionKind.OVERDUE_TASK))
    }

    @Test
    fun `task due today is not overdue`() {
        val items = evaluate(tasks = listOf(task(dueDate = today.atTime(23, 0))))

        assertNull(items.ofKind(AttentionKind.OVERDUE_TASK))
    }

    @Test
    fun `a high priority task is urgent as soon as it slips`() {
        val items = evaluate(
            tasks = listOf(task(priority = TaskPriority.HIGH, dueDate = today.minusDays(1).atStartOfDay()))
        )

        assertEquals(AttentionSeverity.URGENT, items.ofKind(AttentionKind.OVERDUE_TASK)!!.severity)
    }

    @Test
    fun `an ordinary task only just overdue stays calm`() {
        val items = evaluate(tasks = listOf(task(dueDate = today.minusDays(1).atStartOfDay())))

        assertEquals(AttentionSeverity.NEEDS_ATTENTION, items.ofKind(AttentionKind.OVERDUE_TASK)!!.severity)
    }

    // -------------------------------------------------------------- inbox

    @Test
    fun `unprocessed inbox items are reported informationally`() {
        val items = evaluate(inboxCount = 5)

        val item = items.ofKind(AttentionKind.INBOX_UNPROCESSED)
        assertNotNull(item)
        assertEquals("5 things need processing", item!!.detail)
        assertEquals(AttentionSeverity.INFORMATIONAL, item.severity)
    }

    @Test
    fun `a single inbox item is described in the singular`() {
        assertEquals("1 thing needs processing", evaluate(inboxCount = 1).ofKind(AttentionKind.INBOX_UNPROCESSED)!!.detail)
    }

    @Test
    fun `an empty inbox produces nothing`() {
        assertNull(evaluate(inboxCount = 0).ofKind(AttentionKind.INBOX_UNPROCESSED))
    }

    // ----------------------------------------------------------- priorities

    @Test
    fun `choosing more than three priorities is pointed out gently`() {
        val tasks = (1..4).map { task(id = "task-$it", isTopPriority = true) }

        val item = evaluate(tasks = tasks).ofKind(AttentionKind.TOO_MANY_PRIORITIES)
        assertNotNull(item)
        assertEquals(AttentionSeverity.INFORMATIONAL, item!!.severity)
    }

    @Test
    fun `three priorities is fine`() {
        val tasks = (1..3).map { task(id = "task-$it", isTopPriority = true) }

        assertNull(evaluate(tasks = tasks).ofKind(AttentionKind.TOO_MANY_PRIORITIES))
    }

    @Test
    fun `completed priorities do not count towards the limit`() {
        val tasks = (1..5).map {
            task(id = "task-$it", isTopPriority = true, status = TaskStatus.COMPLETED)
        }

        assertNull(evaluate(tasks = tasks).ofKind(AttentionKind.TOO_MANY_PRIORITIES))
    }

    // ------------------------------------------------------ inactive projects

    @Test
    fun `an active project untouched for a fortnight is noted`() {
        val items = evaluate(
            tasks = listOf(task(id = "task-1", projectId = "project-1")),
            projects = listOf(project(nextActionId = "task-1", updatedAt = today.minusDays(20).atStartOfDay()))
        )

        val item = items.ofKind(AttentionKind.PROJECT_INACTIVE)
        assertNotNull(item)
        assertEquals(AttentionSeverity.INFORMATIONAL, item!!.severity)
    }

    @Test
    fun `a recently touched project is not flagged as inactive`() {
        val items = evaluate(
            tasks = listOf(task(id = "task-1", projectId = "project-1")),
            projects = listOf(project(nextActionId = "task-1", updatedAt = today.minusDays(3).atStartOfDay()))
        )

        assertNull(items.ofKind(AttentionKind.PROJECT_INACTIVE))
    }

    // ------------------------------------------------------------- ordering

    @Test
    fun `the most severe items are listed first`() {
        val items = evaluate(
            tasks = listOf(task(id = "task-1", dueDate = today.minusDays(1).atStartOfDay())),
            waitingItems = listOf(waiting(followUpDate = today.minusDays(10).atStartOfDay())),
            inboxCount = 2
        )

        val severities = items.map { it.severity }
        assertEquals(AttentionSeverity.URGENT, severities.first())
        assertEquals(
            "severities should never increase as the list goes on",
            severities.sortedByDescending { it.ordinal },
            severities
        )
    }

    @Test
    fun `a quiet system produces no attention items at all`() {
        val items = evaluate(
            tasks = listOf(task(id = "task-1", projectId = "project-1")),
            projects = listOf(project(nextActionId = "task-1")),
            waitingItems = listOf(waiting(followUpDate = today.plusDays(3).atStartOfDay())),
            inboxCount = 0
        )

        assertTrue("nothing is wrong, so nothing should be raised: $items", items.isEmpty())
    }

    @Test
    fun `attention item ids are unique so the ui can key on them`() {
        val items = evaluate(
            tasks = listOf(
                task(id = "task-1", projectId = "project-1", dueDate = today.minusDays(2).atStartOfDay()),
                task(id = "task-2", projectId = "project-1", dueDate = today.minusDays(4).atStartOfDay())
            ),
            projects = listOf(project()),
            waitingItems = listOf(
                waiting(id = "waiting-1", followUpDate = today.minusDays(1).atStartOfDay()),
                waiting(id = "waiting-2", followUpDate = today.minusDays(8).atStartOfDay())
            ),
            inboxCount = 3
        )

        assertEquals(items.map { it.id }.distinct().size, items.size)
    }

    // ------------------------------------------------------- project scoping

    @Test
    fun `project evaluation only reports that project's own problems`() {
        val project = project(id = "project-1", nextActionId = null)
        val items = AttentionEngine.evaluateForProject(
            project = project,
            projectTasks = listOf(task(id = "task-1", projectId = "project-1")),
            projectWaiting = listOf(waiting(followUpDate = today.minusDays(2).atStartOfDay(), projectId = "project-1")),
            today = today
        )

        assertNotNull(items.ofKind(AttentionKind.PROJECT_NO_NEXT_ACTION))
        assertNotNull(items.ofKind(AttentionKind.WAITING_FOLLOW_UP_OVERDUE))
        assertNull("the inbox is not a project concern", items.ofKind(AttentionKind.INBOX_UNPROCESSED))
    }
}
