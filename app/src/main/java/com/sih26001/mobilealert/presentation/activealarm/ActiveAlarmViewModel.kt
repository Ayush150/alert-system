package com.sih26001.mobilealert.presentation.activealarm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.Alert
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActiveAlarmViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val alertRepository = DependencyContainer.alertRepository

    // Extract the alertId from the navigation route
    val alertId: String = checkNotNull(savedStateHandle["alertId"])

    // Expose the alert directly by observing it from the repository
    val alert: StateFlow<Alert?> = alertRepository.getAlertById(alertId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun silenceAlert() {
        viewModelScope.launch {
            alertRepository.silenceAlert(alertId)
        }
    }
    
    fun acknowledgeAlert() {
        viewModelScope.launch {
            alertRepository.acknowledgeAlert(alertId)
        }
    }
}
