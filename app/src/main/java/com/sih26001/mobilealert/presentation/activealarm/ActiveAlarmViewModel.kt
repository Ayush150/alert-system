package com.sih26001.mobilealert.presentation.activealarm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.repository.AlertRepository
import com.sih26001.mobilealert.domain.usecase.AcknowledgeAlertUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActiveAlarmViewModel(
    savedStateHandle: SavedStateHandle,
    private val alertRepository: AlertRepository = DependencyContainer.alertRepository,
    private val acknowledgeAlertUseCase: AcknowledgeAlertUseCase = DependencyContainer.acknowledgeAlertUseCase
) : ViewModel() {

    // Extract the alertId from the navigation route
    val alertId: String = checkNotNull(savedStateHandle["alertId"])

    // Expose the alert directly by observing it from the repository
    val alert: StateFlow<Alert?> = alertRepository.getAlertById(alertId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Observes whether this alert is queued in Room for future backend synchronization
    val isAckPending: StateFlow<Boolean> = alertRepository.observePendingAckIds()
        .map { it.contains(alertId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Observes the detailed synchronization state of this alert's acknowledgement
    val ackSyncStatus: StateFlow<com.sih26001.mobilealert.data.local.AckSyncStatus?> = alertRepository.observeAckSyncStatus(alertId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _ackError = MutableStateFlow<String?>(null)
    val ackError: StateFlow<String?> = _ackError.asStateFlow()

    fun silenceAlert() {
        viewModelScope.launch {
            alertRepository.silenceAlert(alertId)
        }
    }
    
    fun acknowledgeAlert() {
        viewModelScope.launch {
            _ackError.value = null
            val result = acknowledgeAlertUseCase(alertId)
            result.onFailure { error ->
                _ackError.value = error.message ?: "Acknowledgement failed"
            }
        }
    }
}
