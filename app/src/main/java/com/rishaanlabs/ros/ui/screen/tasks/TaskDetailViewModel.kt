package com.rishaanlabs.ros.ui.screen.tasks

import androidx.lifecycle.SavedStateHandle
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
import java.time.LocalDateTime
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String? = savedStateHandle["taskId"]
    private val _state = MutableStateFlow(TaskDetailUiState())
    val state: StateFlow<TaskDetailUiState> = _state.asStateFlow()

    val projects = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (taskId != null) {
            viewModelScope.launch {
                val task = taskRepository.getById(taskId)
                _state.update { it.copy(task = task) }
            }
        }
    }

    fun save(
        title: String,
        description: String,
        priority: TaskPriority,
        projectId: String?,
        dueDate: LocalDateTime?,
        isTopPriority: Boolean,
        isSomeday: Boolean
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val existing = _state.value.task
            if (existing == null) {
                taskRepository.create(title = title.trim(), description = description.trim(), projectId = projectId, priority = priority)
            } else {
                taskRepository.update(
                    existing.copy(
                        title = title.trim(),
                        description = description.trim(),
                        priority = priority,
                        projectId = projectId,
                        dueDate = dueDate,
                        isTopPriority = isTopPriority,
                        isSomeday = isSomeday
                    )
                )
            }
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun delete() = viewModelScope.launch {
        _state.value.task?.let { taskRepository.delete(it) }
        _state.update { it.copy(isSaved = true) }
    }
}
