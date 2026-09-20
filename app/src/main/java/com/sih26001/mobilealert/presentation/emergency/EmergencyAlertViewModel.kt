package com.sih26001.mobilealert.presentation.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.safeplace.SafePlaceResolver
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.SafePlaceDestination
import com.sih26001.mobilealert.domain.repository.AlertRepository
import com.sih26001.mobilealert.domain.usecase.AcknowledgeAlertUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmergencyAlertViewModel(
    val alertId: String,
    private val alertRepository: AlertRepository = DependencyContainer.alertRepository,
    private val acknowledgeAlertUseCase: AcknowledgeAlertUseCase = DependencyContainer.acknowledgeAlertUseCase,
    private val alarmController: AlarmController = DependencyContainer.alarmController
) : ViewModel() {

    // Observes the authoritative alert from Room
    val alert: StateFlow<Alert?> = alertRepository.getAlertById(alertId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Observes whether the hardware siren is actively sounding
    val isSirenActive: StateFlow<Boolean> = alarmController.isSirenActive

    // Resolved safe destination (authoritative or fixed demo with demo indicators)
    val destination: StateFlow<SafePlaceDestination> = alert
        .map { SafePlaceResolver.resolveDestination(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SafePlaceResolver.resolveDestination(null)
        )

    // Observes whether ACK is queued for sync
    val isAckPending: StateFlow<Boolean> = alertRepository.observePendingAckIds()
        .map { it.contains(alertId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Tracks if alert has been acknowledged
    val isAcknowledged: StateFlow<Boolean> = alert
        .map { it?.status == AlertStatus.ACKNOWLEDGED }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _ackError = MutableStateFlow<String?>(null)
    val ackError: StateFlow<String?> = _ackError.asStateFlow()

    /**
     * Stops ONLY the siren sound/vibration.
     * Alert remains ACTIVE, visible, and unacknowledged.
     */
    fun silenceSiren() {
        alarmController.silenceAlarm(alertId)
        viewModelScope.launch {
            alertRepository.silenceAlert(alertId)
        }
    }

    /**
     * Persists user acknowledgement in Room and syncs to backend.
     * Also stops hardware siren if active.
     */
    fun acknowledgeAlert() {
        alarmController.stopAlarm()
        viewModelScope.launch {
            _ackError.value = null
            val result = acknowledgeAlertUseCase(alertId)
            result.onFailure { error ->
                _ackError.value = error.message ?: "Acknowledgement failed"
            }
        }
    }
}
