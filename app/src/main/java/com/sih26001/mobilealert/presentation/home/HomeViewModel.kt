package com.sih26001.mobilealert.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sih26001.mobilealert.data.local.toEntity
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.Location
import com.sih26001.mobilealert.domain.repository.AlertRepository
import com.sih26001.mobilealert.domain.usecase.AcknowledgeAlertUseCase
import com.sih26001.mobilealert.domain.usecase.GetActiveAlertsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    getActiveAlertsUseCase: GetActiveAlertsUseCase = GetActiveAlertsUseCase(alertRepository)
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _isOffline = MutableStateFlow(false)
    private val _lastUpdated = MutableStateFlow<Instant?>(null)
    private val _ackMessage = MutableStateFlow<String?>(null)

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    val uiState: StateFlow<HomeUiState> = combine(
        getActiveAlertsUseCase(),
        alertRepository.observePendingAckIds(),
        _isRefreshing,
        _isOffline,
        _lastUpdated
    ) { activeAlerts: List<Alert>, pendingAcks: Set<String>, refreshing: Boolean, offline: Boolean, updatedInstant: Instant? ->
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
        val updatedText = updatedInstant?.let { timeFormatter.format(it) }

        HomeUiState(
            isLoading = refreshing,
            primaryAlert = primary,
            allActiveAlerts = sortedAlerts,
            isOffline = offline,
            lastUpdatedText = updatedText,
            monitoredArea = area,
            pendingAckIds = pendingAcks,
            ackMessage = _ackMessage.value
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
        viewModelScope.launch {
            alertRepository.silenceAlert(alertId)
        }
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            val result = acknowledgeAlertUseCase(alertId)
            result.onFailure { error ->
                _ackMessage.value = error.message ?: "Acknowledgement failed"
            }
        }
    }

    // --- Demo Protocol Simulations (Section 17) ---

    fun triggerDemoWarning() {
        viewModelScope.launch {
            val warningAlert = Alert(
                alertId = "ALT-DEMO-WARN",
                eventType = "LANDSLIDE_RISK",
                severity = AlertSeverity.HIGH,
                riskScore = 0.72,
                location = Location("Shillong", 25.5788, 91.8933),
                issuedAt = Instant.now(),
                expiresAt = Instant.now().plusSeconds(7200),
                topDrivers = listOf(
                    "Heavy rainfall",
                    "High soil moisture",
                    "Increased ground movement"
                ),
                recommendedAction = "Stay away from steep slopes and avoid travelling toward the affected area.",
                affectedAssets = listOf(
                    com.sih26001.mobilealert.domain.model.AffectedAsset("road", "GS Road Sector 4")
                ),
                source = "sih26001_demo",
                dataQuality = "GOOD",
                requiresAck = false,
                status = AlertStatus.ACTIVE
            )
            DependencyContainer.database.alertDao().insertAlert(warningAlert.toEntity())
            _lastUpdated.value = Instant.now()
        }
    }

    fun triggerDemoHighAlert() {
        viewModelScope.launch {
            val highAlert = Alert(
                alertId = "ALT-DEMO-CRIT",
                eventType = "LANDSLIDE_DANGER",
                severity = AlertSeverity.CRITICAL,
                riskScore = 0.94,
                location = Location("Shillong", 25.5788, 91.8933),
                issuedAt = Instant.now(),
                expiresAt = Instant.now().plusSeconds(3600),
                topDrivers = listOf(
                    "Extreme slope displacement detected",
                    "Pore water pressure threshold exceeded",
                    "Severe continuous precipitation"
                ),
                recommendedAction = "Move away from the affected area and go to a safe location.",
                affectedAssets = listOf(
                    com.sih26001.mobilealert.domain.model.AffectedAsset("road", "NH-13 Shillong Bypass"),
                    com.sih26001.mobilealert.domain.model.AffectedAsset("settlement", "Laitumkhrah Ward 3"),
                    com.sih26001.mobilealert.domain.model.AffectedAsset("shelter", "Shillong Civil Defense Relief Center")
                ),
                source = "sih26001_demo",
                dataQuality = "GOOD",
                requiresAck = true,
                status = AlertStatus.ACTIVE
            )
            DependencyContainer.database.alertDao().insertAlert(highAlert.toEntity())
            _lastUpdated.value = Instant.now()
        }
    }

    fun triggerDemoNormal() {
        viewModelScope.launch {
            DependencyContainer.database.alertDao().deleteAllAlerts()
            _lastUpdated.value = Instant.now()
        }
    }
}
