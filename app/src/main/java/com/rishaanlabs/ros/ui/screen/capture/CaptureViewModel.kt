package com.rishaanlabs.ros.ui.screen.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rishaanlabs.ros.data.local.entity.InboxItemType
import com.rishaanlabs.ros.data.repository.InboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val inboxRepository: InboxRepository
) : ViewModel() {

    fun capture(text: String, type: InboxItemType = InboxItemType.UNSPECIFIED) {
        if (text.isBlank()) return
        viewModelScope.launch {
            inboxRepository.capture(text.trim(), type)
        }
    }
}
