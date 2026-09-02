package com.rishaanlabs.ros.ui.screen.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.InboxItem
import com.rishaanlabs.ros.data.local.entity.Project
import com.rishaanlabs.ros.data.local.entity.TaskPriority
import com.rishaanlabs.ros.data.repository.InboxRepository
import com.rishaanlabs.ros.data.repository.NoteRepository
import com.rishaanlabs.ros.data.repository.ProjectRepository
import com.rishaanlabs.ros.data.repository.TaskRepository
import com.rishaanlabs.ros.data.repository.WaitingRepository
import com.rishaanlabs.ros.domain.capture.CapturedText
import com.rishaanlabs.ros.domain.capture.splitCapturedText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Everything the Inbox screen needs, in one object so the UI never renders a half-loaded state. */
data class InboxUiState(
    val unprocessed: List<InboxItem> = emptyList(),
    val all: List<InboxItem> = emptyList(),
    val projects: List<Project> = emptyList(),
    val loaded: Boolean = false
) {
    val remaining: Int get() = unprocessed.size
    val current: InboxItem? get() = unprocessed.firstOrNull()
}

/** What the user decided a captured thought actually was. */
enum class ProcessDestination { TASK, WAITING, NOTE }

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val inboxRepository: InboxRepository,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val waitingRepository: WaitingRepository,
    projectRepository: ProjectRepository
) : ViewModel() {

    val uiState = combine(
        inboxRepository.observeUnprocessed(),
        inboxRepository.observeAll(),
        projectRepository.observeActive()
    ) { unprocessed, all, projects ->
        InboxUiState(unprocessed = unprocessed, all = all, projects = projects, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    /**
     * Turns one captured thought into a real record.
     *
     * All the conversion paths live here rather than in the screen so that processing an item and
     * marking it processed can never come apart — the item is only retired once its replacement
     * exists.
     */
    fun process(
        item: InboxItem,
        destination: ProcessDestination,
        projectId: String? = null,
        date: LocalDate? = null,
        priority: TaskPriority = TaskPriority.NONE,
        person: String = "",
        titleOverride: String? = null
    ) = viewModelScope.launch {
        // One parse for every destination. A capture that reads as a heading with lines under it
        // keeps that shape whether it becomes a task, a waiting item or a note; before this, only
        // notes split the text and the other two turned the whole blob into a title.
        val parsed = splitCapturedText(item.text)
        val title = titleOverride?.trim()?.ifBlank { null } ?: parsed.proposedTitle
        // Keeping the proposed title consumes the first line; replacing it means that line was an
        // item after all, so it stays in the body.
        val body = parsed.bodyFor(title)

        when (destination) {
            ProcessDestination.TASK -> {
                val task = taskRepository.create(
                    title = title,
                    description = body,
                    projectId = projectId,
                    priority = priority
                )
                if (date != null) {
                    taskRepository.update(task.copy(dueDate = date.atStartOfDay()))
                }
            }

            ProcessDestination.WAITING -> waitingRepository.create(
                title = title,
                person = person,
                description = body,
                projectId = projectId,
                followUpDate = date?.atStartOfDay()
            )

            ProcessDestination.NOTE -> noteRepository.create(
                title = title,
                body = body,
                projectId = projectId
            )
        }
        inboxRepository.markProcessed(item)
    }

    /**
     * What the processor should show for an item: the title to offer, and whether to ask before
     * committing it. The screen calls this rather than parsing the text itself, so the title the
     * user confirms is the one [process] would otherwise have used.
     */
    fun preview(item: InboxItem): CapturedText = splitCapturedText(item.text)

    /** The item was not worth keeping. */
    fun delete(item: InboxItem) = viewModelScope.launch { inboxRepository.delete(item) }

    /** Retire an item without creating anything — it has already been dealt with. */
    fun markProcessed(item: InboxItem) = viewModelScope.launch {
        inboxRepository.markProcessed(item)
    }
}
