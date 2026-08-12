package com.rishaanlabs.ros.ui.screen.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.InboxItem
import com.rishaanlabs.ros.data.local.entity.InboxItemType
import com.rishaanlabs.ros.data.repository.InboxRepository
import com.rishaanlabs.ros.data.repository.NoteRepository
import com.rishaanlabs.ros.data.repository.TaskRepository
import com.rishaanlabs.ros.data.repository.WaitingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val inboxRepository: InboxRepository,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val waitingRepository: WaitingRepository
) : ViewModel() {

    val items = inboxRepository.observeUnprocessed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(item: InboxItem) = viewModelScope.launch { inboxRepository.delete(item) }

    fun convertToTask(item: InboxItem) = viewModelScope.launch {
        taskRepository.create(title = item.text)
        inboxRepository.markProcessed(item)
    }

    fun convertToNote(item: InboxItem) = viewModelScope.launch {
        noteRepository.create(title = item.text)
        inboxRepository.markProcessed(item)
    }

    fun convertToWaiting(item: InboxItem) = viewModelScope.launch {
        waitingRepository.create(title = item.text)
        inboxRepository.markProcessed(item)
    }

    fun markProcessed(item: InboxItem) = viewModelScope.launch {
        inboxRepository.markProcessed(item)
    }
}
