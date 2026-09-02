package com.rishaanlabs.ros.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.repository.InboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The only state the shell itself needs: how much is waiting in the inbox.
 *
 * It lives here rather than being lifted from Home's state because the bottom bar outlives any
 * one screen — the badge has to stay right while the user is three levels deep in Finance.
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    inboxRepository: InboxRepository
) : ViewModel() {

    val inboxCount = inboxRepository.observeUnprocessedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
