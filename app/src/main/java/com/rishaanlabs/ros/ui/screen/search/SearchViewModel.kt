package com.rishaanlabs.ros.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.Note
import com.rishaanlabs.ros.data.local.entity.Project
import com.rishaanlabs.ros.data.local.entity.Task
import com.rishaanlabs.ros.data.local.entity.WaitingItem
import com.rishaanlabs.ros.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResults(
    val tasks: List<Task> = emptyList(),
    val notes: List<Note> = emptyList(),
    val waiting: List<WaitingItem> = emptyList()
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val waitingRepository: WaitingRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow(SearchResults())
    val results: StateFlow<SearchResults> = _results.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(300)
                .filter { it.length >= 2 }
                .collect { q ->
                    val tasks = taskRepository.search(q)
                    val notes = noteRepository.search(q)
                    val waiting = waitingRepository.search(q)
                    _results.value = SearchResults(tasks = tasks, notes = notes, waiting = waiting)
                }
        }
    }

    fun setQuery(q: String) {
        _query.value = q
        if (q.length < 2) _results.value = SearchResults()
    }
}
