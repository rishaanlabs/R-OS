package com.rishaanlabs.ros.ui.screen.projects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.Project
import com.rishaanlabs.ros.data.local.entity.ProjectStatus
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.Note
import com.rishaanlabs.ros.data.local.entity.WaitingItem
import com.rishaanlabs.ros.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectDetailUiState(
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val notes: List<Note> = emptyList(),
    val waitingItems: List<WaitingItem> = emptyList(),
    val isSaved: Boolean = false
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val waitingRepository: WaitingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: String? = savedStateHandle["projectId"]
    private val _state = MutableStateFlow(ProjectDetailUiState())
    val state: StateFlow<ProjectDetailUiState> = _state.asStateFlow()

    init {
        if (projectId != null) {
            viewModelScope.launch {
                combine(
                    projectRepository.observeById(projectId),
                    taskRepository.observeByProject(projectId),
                    noteRepository.observeByProject(projectId),
                    waitingRepository.observeByProject(projectId)
                ) { project, tasks, notes, waiting ->
                    _state.value.copy(project = project, tasks = tasks, notes = notes, waitingItems = waiting)
                }.collect { newState -> _state.value = newState }
            }
        }
    }

    fun save(title: String, description: String, status: ProjectStatus) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val existing = _state.value.project
            if (existing == null) {
                projectRepository.create(title = title.trim(), description = description.trim())
            } else {
                projectRepository.update(existing.copy(title = title.trim(), description = description.trim(), status = status))
            }
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun delete() = viewModelScope.launch {
        _state.value.project?.let { projectRepository.delete(it) }
        _state.update { it.copy(isSaved = true) }
    }

    fun setNextAction(taskId: String?) = viewModelScope.launch {
        projectId?.let { projectRepository.setNextAction(it, taskId) }
    }

    fun completeTask(task: Task) = viewModelScope.launch { taskRepository.complete(task) }
}
