package com.rishaanlabs.ros.ui.screen.waiting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.WaitingItem
import com.rishaanlabs.ros.data.repository.ProjectRepository
import com.rishaanlabs.ros.data.repository.WaitingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class WaitingListViewModel @Inject constructor(
    private val waitingRepository: WaitingRepository
) : ViewModel() {
    val items = waitingRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolve(item: WaitingItem) = viewModelScope.launch { waitingRepository.resolve(item) }
    fun delete(item: WaitingItem) = viewModelScope.launch { waitingRepository.delete(item) }
}

data class WaitingDetailUiState(val item: WaitingItem? = null, val isSaved: Boolean = false)

@HiltViewModel
class WaitingDetailViewModel @Inject constructor(
    private val waitingRepository: WaitingRepository,
    private val projectRepository: ProjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val waitingId: String? = savedStateHandle["waitingId"]
    private val _state = MutableStateFlow(WaitingDetailUiState())
    val state: StateFlow<WaitingDetailUiState> = _state.asStateFlow()
    val projects = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (waitingId != null) {
            viewModelScope.launch {
                val item = waitingRepository.getById(waitingId)
                _state.update { it.copy(item = item) }
            }
        }
    }

    fun save(title: String, person: String, description: String, projectId: String?, followUpDate: LocalDateTime?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val existing = _state.value.item
            if (existing == null) {
                waitingRepository.create(title.trim(), person.trim(), description.trim(), projectId, followUpDate)
            } else {
                waitingRepository.update(existing.copy(
                    title = title.trim(), person = person.trim(),
                    description = description.trim(), projectId = projectId, followUpDate = followUpDate
                ))
            }
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun resolve() = viewModelScope.launch {
        _state.value.item?.let { waitingRepository.resolve(it) }
        _state.update { it.copy(isSaved = true) }
    }

    fun delete() = viewModelScope.launch {
        _state.value.item?.let { waitingRepository.delete(it) }
        _state.update { it.copy(isSaved = true) }
    }
}
