package com.rishaanlabs.ros.ui.screen.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.DailyReview
import com.rishaanlabs.ros.data.repository.DailyReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DailyReviewUiState(
    val review: DailyReview? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class DailyReviewViewModel @Inject constructor(
    private val repository: DailyReviewRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DailyReviewUiState())
    val state: StateFlow<DailyReviewUiState> = _state.asStateFlow()
    val history = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.observeByDate(LocalDate.now()).collect { existing ->
                _state.update { it.copy(review = existing) }
            }
        }
    }

    fun save(energy: Int?, mainFocus: String, biggestWin: String, unfinished: String, toRemember: String, tomorrowPriority: String) {
        viewModelScope.launch {
            val review = DailyReview(
                id = _state.value.review?.id ?: java.util.UUID.randomUUID().toString(),
                date = LocalDate.now(),
                energy = energy,
                mainFocus = mainFocus.trim(),
                biggestWin = biggestWin.trim(),
                unfinished = unfinished.trim(),
                toRemember = toRemember.trim(),
                tomorrowPriority = tomorrowPriority.trim()
            )
            repository.save(review)
            _state.update { it.copy(isSaved = true) }
        }
    }
}
