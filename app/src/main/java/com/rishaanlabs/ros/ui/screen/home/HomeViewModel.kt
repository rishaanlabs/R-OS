package com.rishaanlabs.ros.ui.screen.home

import androidx.lifecycle.ViewModel
import com.rishaanlabs.ros.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class HomeUiState(
    val greeting: String = "",
    val topPriorityTasks: List<com.rishaanlabs.ros.data.local.entity.Task> = emptyList(),
    val todayTasks: List<com.rishaanlabs.ros.data.local.entity.Task> = emptyList(),
    val waitingCount: Int = 0,
    val waitingOverdueCount: Int = 0,
    val inboxCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    taskRepository: TaskRepository,
    waitingRepository: WaitingRepository,
    inboxRepository: InboxRepository
) : ViewModel() {

    val uiState = combine(
        taskRepository.observeTopPriorities(),
        taskRepository.observeToday(),
        waitingRepository.observeActiveCount(),
        waitingRepository.observeOverdueCount(),
        inboxRepository.observeUnprocessedCount()
    ) { priorities, today, waitingCount, overdueCount, inboxCount ->
        HomeUiState(
            greeting = buildGreeting(),
            topPriorityTasks = priorities,
            todayTasks = today,
            waitingCount = waitingCount,
            waitingOverdueCount = overdueCount,
            inboxCount = inboxCount
        )
    }

    private fun buildGreeting(): String {
        val hour = java.time.LocalTime.now().hour
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
