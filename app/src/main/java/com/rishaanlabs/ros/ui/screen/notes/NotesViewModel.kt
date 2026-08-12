package com.rishaanlabs.ros.ui.screen.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.Note
import com.rishaanlabs.ros.data.repository.NoteRepository
import com.rishaanlabs.ros.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    val notes = noteRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(note: Note) = viewModelScope.launch { noteRepository.delete(note) }
}

data class NoteDetailUiState(val note: Note? = null, val isSaved: Boolean = false)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val projectRepository: ProjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String? = savedStateHandle["noteId"]
    private val _state = MutableStateFlow(NoteDetailUiState())
    val state: StateFlow<NoteDetailUiState> = _state.asStateFlow()
    val projects = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (noteId != null) {
            viewModelScope.launch {
                noteRepository.observeById(noteId).collect { note ->
                    _state.update { it.copy(note = note) }
                }
            }
        }
    }

    fun save(title: String, body: String, projectId: String?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val existing = _state.value.note
            if (existing == null) {
                noteRepository.create(title.trim(), body.trim(), projectId)
            } else {
                noteRepository.update(existing.copy(title = title.trim(), body = body.trim(), projectId = projectId))
            }
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun delete() = viewModelScope.launch {
        _state.value.note?.let { noteRepository.delete(it) }
        _state.update { it.copy(isSaved = true) }
    }
}
