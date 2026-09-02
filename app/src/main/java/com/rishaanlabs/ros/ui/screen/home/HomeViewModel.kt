package com.rishaanlabs.ros.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.TaskStatus
import com.rishaanlabs.ros.data.local.entity.WaitingItem
import com.rishaanlabs.ros.data.local.entity.WaitingStatus
import com.rishaanlabs.ros.data.repository.FinanceRepository
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
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Everything the Home briefing shows, assembled once.
 *
 * Home reads from five different tables. Exposing those as five independent flows made the screen
 * assemble itself in pieces as each one arrived, so they are combined into a single immutable
 * state object and shared as a StateFlow — the screen either has a briefing or it does not.
 */
/** One line of the Home waiting list: who owes what, and how long it has been. */
data class WaitingSummary(
    val item: WaitingItem,
    val daysWaiting: Long
) {
    /** The design turns red past a week — long enough that chasing it is the next action. */
    val isStale: Boolean get() = daysWaiting > 5
}

/**
 * The finance line on Home: position first, then how far through the month's plan the user is.
 *
 * Everything here is derived from figures Finance already computes. There is deliberately no
 * "upcoming payments" list, which the design also shows — scheduled payments have no table, and
 * inventing one is a migration.
 */
data class HomeFinance(
    val currency: String = "MVR",
    val totalCashMinor: Long = 0L,
    val freeMinor: Long = 0L,
    val plannedMinor: Long = 0L,
    val spentAgainstPlanMinor: Long = 0L,
    val hasAccounts: Boolean = false
) {
    /** How much of the month's limits is used. Null when no limit has been set to measure against. */
    val planPercent: Int?
        get() = if (plannedMinor <= 0L) null else
            ((spentAgainstPlanMinor.toDouble() / plannedMinor.toDouble()) * 100).toInt().coerceIn(0, 999)
}

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
    val waiting: List<WaitingSummary> = emptyList(),
    val finance: HomeFinance = HomeFinance(),
    val loaded: Boolean = false
) {
    val unfinishedCount: Int get() = unfinished.size
}

/** The task-side half of Home, before the finance line is folded in. */
private data class HomeCore(
    val priorities: List<Task>,
    val other: List<Task>,
    val attention: List<AttentionItem>,
    val inboxCount: Int,
    val unfinished: List<Task>,
    val waiting: List<WaitingSummary>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    waitingRepository: WaitingRepository,
    inboxRepository: InboxRepository,
    projectRepository: ProjectRepository,
    financeRepository: FinanceRepository
) : ViewModel() {

    private val monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay()
    private val nextMonthStart = monthStart.plusMonths(1)

    /**
     * The finance line, from figures Finance already derives.
     *
     * "Free" is cash minus what goals have earmarked, matching Finance's own definition so Home
     * and Finance cannot quote different numbers. The plan is measured only against categories
     * that actually carry a limit — including unlimited ones would make the percentage mean
     * nothing.
     */
    private val financeLine = combine(
        financeRepository.observeAccountBalances(),
        financeRepository.observeGoalProgress(),
        financeRepository.observeCategorySpend(monthStart, nextMonthStart, HOME_CURRENCY)
    ) { accounts, goals, spend ->
        val inCurrency = accounts.filter { it.account.currency.equals(HOME_CURRENCY, ignoreCase = true) }
        val cash = inCurrency.sumOf { it.balanceMinor }
        val earmarked = goals
            .filter { it.goal.currency.equals(HOME_CURRENCY, ignoreCase = true) }
            .sumOf { it.currentMinor.coerceAtLeast(0L) }
        val limited = spend.filter { (it.monthlyBudgetMinor ?: 0L) > 0L }

        HomeFinance(
            currency = HOME_CURRENCY,
            totalCashMinor = cash,
            freeMinor = cash - earmarked,
            plannedMinor = limited.sumOf { it.monthlyBudgetMinor ?: 0L },
            spentAgainstPlanMinor = limited.sumOf { it.amountMinor },
            hasAccounts = inCurrency.isNotEmpty()
        )
    }

    private val core = combine(
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

        HomeCore(
            priorities = priorities,
            other = other,
            // The inbox gets its own section on Home, so it is not repeated inside the
            // attention list.
            attention = allAttention.filterNot { it.kind == AttentionKind.INBOX_UNPROCESSED },
            inboxCount = inboxCount,
            unfinished = unfinished,
            waiting = waiting
                .filter { it.status == WaitingStatus.WAITING || it.status == WaitingStatus.FOLLOW_UP_DUE }
                .map {
                    WaitingSummary(
                        item = it,
                        daysWaiting = ChronoUnit.DAYS.between(it.requestedDate.toLocalDate(), today)
                            .coerceAtLeast(0L)
                    )
                }
                .sortedByDescending { it.daysWaiting }
        )
    }

    val uiState = combine(core, financeLine) { c, finance ->
        HomeUiState(
            greeting = greetingFor(LocalTime.now()),
            date = LocalDate.now(),
            topPriorities = c.priorities,
            otherTasks = c.other,
            attention = c.attention,
            inboxCount = c.inboxCount,
            unfinished = c.unfinished,
            // A priority can be chosen from anything open and relevant today, not only from what
            // today's query happened to return.
            priorityCandidates = (c.priorities + c.other + c.unfinished).distinctBy { it.id },
            waiting = c.waiting,
            finance = finance,
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

/** Home shows one currency. Multi-currency needs a picker, which is a Finance decision. */
private const val HOME_CURRENCY = "MVR"
