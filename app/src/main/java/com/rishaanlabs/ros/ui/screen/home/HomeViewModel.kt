package com.rishaanlabs.ros.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.TaskStatus
import com.rishaanlabs.ros.data.repository.InboxRepository
import com.rishaanlabs.ros.data.repository.ProjectRepository
import com.rishaanlabs.ros.data.repository.TaskRepository
import com.rishaanlabs.ros.data.repository.WaitingRepository
import com.rishaanlabs.ros.domain.attention.AttentionEngine
import com.rishaanlabs.ros.domain.attention.AttentionItem
import com.rishaanlabs.ros.domain.attention.AttentionKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Everything the Home briefing shows, assembled once.
 *
 * Home reads from five different tables. Exposing those as five independent flows made the screen
 * assemble itself in pieces as each one arrived, so they are combined into a single immutable
 * state object and shared as a StateFlow — the screen either has a briefing or it does not.
 */
data class HomeUiState(
    val greeting: String = "",
    val date: LocalDate = LocalDate.now(),
    val topPriorities: List<Task> = emptyList(),
    val otherTasks: List<Task> = emptyList(),
    val attention: List<AttentionItem> = emptyList(),
    val inboxCount: Int = 0,
    /** Open work that was meant to happen on or before today — the Plan My Day review list. */
    val unfinished: List<Task> = emptyList(),
    /** Everything Plan My Day may offer as a priority. */
    val priorityCandidates: List<Task> = emptyList(),
    val loaded: Boolean = false
) {
    val unfinishedCount: Int get() = unfinished.size
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    waitingRepository: WaitingRepository,
    inboxRepository: InboxRepository,
    projectRepository: ProjectRepository
) : ViewModel() {

    val uiState = combine(
        taskRepository.observeAllOpen(),
        taskRepository.observeToday(),
        waitingRepository.observeAll(),
        projectRepository.observeAll(),
        inboxRepository.observeUnprocessedCount()
    ) { openTasks, todayTasks, waiting, projects, inboxCount ->
        val today = LocalDate.now()

        val priorities = openTasks
            .filter { it.isTopPriority }
            .sortedBy { it.sortOrder }

        // "Other tasks" is what today holds beyond the chosen few, so the two sections together
        // describe the day without repeating a task in both.
        val priorityIds = priorities.map { it.id }.toSet()
        val other = todayTasks.filter { it.id !in priorityIds }

        val allAttention = AttentionEngine.evaluate(
            tasks = openTasks,
            projects = projects,
            waiting = waiting,
            inboxUnprocessedCount = inboxCount,
            today = today
        )

        // Computed here rather than in the sheet so the count Home shows and the list Plan My Day
        // reviews can never disagree — they are the same list.
        val unfinished = unfinishedCandidates(openTasks, today)

        HomeUiState(
            greeting = greetingFor(LocalTime.now()),
            date = today,
            topPriorities = priorities,
            otherTasks = other,
            // The inbox gets its own section on Home, so it is not repeated inside the
            // attention list.
            attention = allAttention.filterNot { it.kind == AttentionKind.INBOX_UNPROCESSED },
            inboxCount = inboxCount,
            unfinished = unfinished,
            // A priority can be chosen from anything open and relevant today, not only from what
            // today's query happened to return.
            priorityCandidates = (priorities + other + unfinished).distinctBy { it.id },
            loaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Toggles a task in or out of today's chosen few. */
    fun toggleTopPriority(task: Task) = viewModelScope.launch {
        taskRepository.update(task.copy(isTopPriority = !task.isTopPriority))
    }

    fun completeTask(task: Task) = viewModelScope.launch {
        taskRepository.complete(task)
    }

    /** Plan My Day, step one: push an unfinished task forward to today. */
    fun scheduleForToday(task: Task) = viewModelScope.launch {
        taskRepository.update(
            task.copy(scheduledDate = LocalDate.now().atStartOfDay(), isSomeday = false)
        )
    }

    /** Plan My Day, step one: push it out of the way for now. */
    fun scheduleForLater(task: Task) = viewModelScope.launch {
        taskRepository.update(
            task.copy(
                scheduledDate = LocalDate.now().plusDays(1).atStartOfDay(),
                isTopPriority = false,
                isSomeday = false
            )
        )
    }

    /** Plan My Day, step one: it matters, but not on any particular day. */
    fun moveToSomeday(task: Task) = viewModelScope.launch {
        taskRepository.update(task.copy(isSomeday = true, isTopPriority = false, scheduledDate = null))
    }

    fun cancelTask(task: Task) = viewModelScope.launch {
        taskRepository.update(task.copy(status = TaskStatus.CANCELLED, isTopPriority = false))
    }

    companion object {
        /**
         * Tasks worth reviewing when planning the day: open work that was meant to happen on or
         * before today. Someday items are excluded — they were explicitly set aside.
         */
        fun unfinishedCandidates(openTasks: List<Task>, today: LocalDate): List<Task> =
            openTasks.filter { task ->
                if (task.isSomeday) return@filter false
                val due = task.dueDate?.toLocalDate()
                val scheduled = task.scheduledDate?.toLocalDate()
                (due != null && !due.isAfter(today)) || (scheduled != null && scheduled.isBefore(today))
            }

        fun greetingFor(time: LocalTime): String = when {
            time.hour < 12 -> "Good morning"
            time.hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
