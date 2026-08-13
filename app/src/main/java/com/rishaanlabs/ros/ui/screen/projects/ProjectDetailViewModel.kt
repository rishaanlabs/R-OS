package com.rishaanlabs.ros.ui.screen.projects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.Note
import com.rishaanlabs.ros.data.local.entity.Project
import com.rishaanlabs.ros.data.local.entity.ProjectStatus
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.TaskStatus
import com.rishaanlabs.ros.data.local.entity.WaitingItem
import com.rishaanlabs.ros.data.local.entity.WaitingStatus
import com.rishaanlabs.ros.data.repository.NoteRepository
import com.rishaanlabs.ros.data.repository.ProjectRepository
import com.rishaanlabs.ros.data.repository.TaskRepository
import com.rishaanlabs.ros.data.repository.WaitingRepository
import com.rishaanlabs.ros.domain.attention.AttentionEngine
import com.rishaanlabs.ros.domain.attention.AttentionItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** One thing that happened in this project, derived from timestamps the entities already carry. */
data class ActivityEntry(
    val id: String,
    val description: String,
    val at: LocalDateTime
)

data class ProjectDetailUiState(
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val notes: List<Note> = emptyList(),
    val waitingItems: List<WaitingItem> = emptyList(),
    val attention: List<AttentionItem> = emptyList(),
    val recentActivity: List<ActivityEntry> = emptyList(),
    val isSaved: Boolean = false
) {
    val openTasks: List<Task> get() = tasks.filter { it.status == TaskStatus.OPEN }
    val completedTasks: List<Task> get() = tasks.filter { it.status == TaskStatus.COMPLETED }

    val openWaiting: List<WaitingItem>
        get() = waitingItems.filter { it.status == WaitingStatus.WAITING || it.status == WaitingStatus.FOLLOW_UP_DUE }

    /**
     * The next action, but only if it is still real. A pointer to a task that has been completed
     * or deleted is treated as absent — a stale next action quietly stops being true, which is
     * worse than having none at all.
     */
    val nextAction: Task?
        get() = project?.nextActionId?.let { id -> openTasks.firstOrNull { it.id == id } }

    val needsNextAction: Boolean
        get() = project?.status == ProjectStatus.ACTIVE && nextAction == null && openTasks.isNotEmpty()
}

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
                    val today = LocalDate.now()
                    _state.value.copy(
                        project = project,
                        tasks = tasks,
                        notes = notes,
                        waitingItems = waiting,
                        attention = project?.let {
                            AttentionEngine.evaluateForProject(it, tasks, waiting, today)
                        } ?: emptyList(),
                        recentActivity = buildRecentActivity(tasks, notes, waiting)
                    )
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
                projectRepository.update(
                    existing.copy(title = title.trim(), description = description.trim(), status = status)
                )
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

    fun completeTask(task: Task) = viewModelScope.launch {
        taskRepository.complete(task)
        // Completing the next action would otherwise leave the project pointing at finished work.
        // Clearing it turns a silent stale pointer into an honest "choose the next one".
        val project = _state.value.project
        if (project != null && project.nextActionId == task.id) {
            projectRepository.setNextAction(project.id, null)
        }
    }

    companion object {
        private const val RECENT_LIMIT = 5

        /**
         * Builds a short activity list from timestamps the V0.1.0 schema already records.
         *
         * This deliberately does not introduce an activity log table. A real history — edits,
         * status changes, what a field used to be — needs its own design, and inventing a
         * half-version of it now would mean a schema migration on a database that already holds
         * the owner's real data. What can be shown honestly from completedAt and createdAt is
         * shown; the rest waits.
         */
        fun buildRecentActivity(
            tasks: List<Task>,
            notes: List<Note>,
            waiting: List<WaitingItem>
        ): List<ActivityEntry> {
            val entries = mutableListOf<ActivityEntry>()

            tasks.forEach { task ->
                task.completedAt?.let {
                    entries += ActivityEntry("task-done-${task.id}", "Completed ${task.title}", it)
                }
            }
            notes.forEach { note ->
                entries += ActivityEntry("note-${note.id}", "Added note ${note.title}", note.createdAt)
            }
            waiting.forEach { item ->
                entries += ActivityEntry("waiting-${item.id}", "Started waiting on ${item.title}", item.createdAt)
                item.resolvedAt?.let {
                    entries += ActivityEntry("waiting-done-${item.id}", "Resolved ${item.title}", it)
                }
            }

            return entries.sortedByDescending { it.at }.take(RECENT_LIMIT)
        }
    }
}
