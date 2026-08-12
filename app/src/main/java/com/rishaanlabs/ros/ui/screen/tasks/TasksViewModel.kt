package com.rishaanlabs.ros.ui.screen.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.TaskPriority
import com.rishaanlabs.ros.data.local.entity.TaskStatus
import com.rishaanlabs.ros.data.repository.ProjectRepository
import com.rishaanlabs.ros.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TasksTab { TODAY, UPCOMING, ALL, SOMEDAY, COMPLETED }

data class TasksUiState(
    val tab: TasksTab = TasksTab.TODAY,
    val tasks: List<Task> = emptyList()
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _tab = MutableStateFlow(TasksTab.TODAY)

    val uiState: StateFlow<TasksUiState> = _tab.flatMapLatest { tab ->
        val flow = when (tab) {
            TasksTab.TODAY -> taskRepository.observeToday()
            TasksTab.UPCOMING -> taskRepository.observeUpcoming()
            TasksTab.ALL -> taskRepository.observeAllOpen()
            TasksTab.SOMEDAY -> taskRepository.observeSomeday()
            TasksTab.COMPLETED -> taskRepository.observeCompleted()
        }
        flow.map { TasksUiState(tab = tab, tasks = it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    fun setTab(tab: TasksTab) { _tab.value = tab }

    fun complete(task: Task) = viewModelScope.launch { taskRepository.complete(task) }
    fun reopen(task: Task) = viewModelScope.launch { taskRepository.reopen(task) }
    fun delete(task: Task) = viewModelScope.launch { taskRepository.delete(task) }
}
