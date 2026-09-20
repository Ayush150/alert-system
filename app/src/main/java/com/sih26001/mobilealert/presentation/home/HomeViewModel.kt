package com.sih26001.mobilealert.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.Location
import com.sih26001.mobilealert.domain.repository.AlertRepository
import com.sih26001.mobilealert.domain.usecase.AcknowledgeAlertUseCase
import com.sih26001.mobilealert.domain.usecase.GetActiveAlertsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val isLoading: Boolean = false,
    val primaryAlert: Alert? = null,
    val allActiveAlerts: List<Alert> = emptyList(),
    val isOffline: Boolean = false,
    val lastUpdatedText: String? = null,
    val monitoredArea: String = "Shillong",
    val pendingAckIds: Set<String> = emptySet(),
    val isAcknowledging: Boolean = false,
    val ackMessage: String? = null
)

class HomeViewModel(
    private val alertRepository: AlertRepository = DependencyContainer.alertRepository,
    private val acknowledgeAlertUseCase: AcknowledgeAlertUseCase = DependencyContainer.acknowledgeAlertUseCase,
    getActiveAlertsUseCase: GetActiveAlertsUseCase = GetActiveAlertsUseCase(alertRepository),
    private val alarmController: AlarmController = DependencyContainer.alarmController
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _isOffline = MutableStateFlow(false)
    private val _lastUpdated = MutableStateFlow<Instant?>(null)
    private val _ackMessage = MutableStateFlow<String?>(null)

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    private data class UiMetadata(
        val refreshing: Boolean,
        val offline: Boolean,
        val lastUpdated: Instant?,
        val ackMessage: String?
    )

    private val uiMetadataFlow = combine(_isRefreshing, _isOffline, _lastUpdated, _ackMessage) { refreshing, offline, updated, ackMsg ->
        UiMetadata(refreshing, offline, updated, ackMsg)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        getActiveAlertsUseCase(),
        alertRepository.observePendingAckIds(),
        uiMetadataFlow
    ) { activeAlerts, pendingAcks, metadata ->
        // Sort active alerts: CRITICAL first, then HIGH, then NORMAL
        val sortedAlerts = activeAlerts.sortedWith(
            compareByDescending<Alert> { alert ->
                when (alert.severity) {
                    AlertSeverity.CRITICAL -> 3
                    AlertSeverity.HIGH -> 2
                    AlertSeverity.NORMAL -> 1
                }
            }.thenByDescending { it.issuedAt }
        )

        val primary = sortedAlerts.firstOrNull()
        val area = primary?.location?.name?.takeIf { it.isNotBlank() } ?: "Shillong"
        val updatedText = metadata.lastUpdated?.let { timeFormatter.format(it) }

        HomeUiState(
            isLoading = metadata.refreshing,
            primaryAlert = primary,
            allActiveAlerts = sortedAlerts,
            isOffline = metadata.offline,
            lastUpdatedText = updatedText,
            monitoredArea = area,
            pendingAckIds = pendingAcks,
            ackMessage = metadata.ackMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = false)
    )

    init {
        refreshAlerts()
    }

    fun refreshAlerts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = alertRepository.refreshAlerts()
            _isRefreshing.value = false
            if (result.isSuccess) {
                _isOffline.value = false
                _lastUpdated.value = Instant.now()
            } else {
                _isOffline.value = true
            }
        }
    }

    fun silenceAlert(alertId: String) {
        alarmController.silenceAlarm(alertId)
        viewModelScope.launch {
            alertRepository.silenceAlert(alertId)
        }
    }

    fun acknowledgeAlert(alertId: String) {
        alarmController.silenceAlarm(alertId)
        viewModelScope.launch {
            val result = acknowledgeAlertUseCase(alertId)
            result.onFailure { error ->
                _ackMessage.value = error.message ?: "Acknowledgement failed"
            }
        }
    }

    fun clearAckMessage() {
        _ackMessage.value = null
    }
}
