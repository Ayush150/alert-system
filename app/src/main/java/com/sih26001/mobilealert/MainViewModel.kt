package com.sih26001.mobilealert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.repository.AlertRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel(
    private val notificationManager: AlertNotificationManager,
    private val alarmController: AlarmController,
    private val alertRepository: AlertRepository = DependencyContainer.alertRepository
) : ViewModel() {

    private val knownActiveAlertIds = mutableSetOf<String>()

    init {
        // App startup: refresh active alerts from remote for dashboard UI synchronization only.
        // App startup NEVER generates alerts, notifications, or alarms.
        viewModelScope.launch {
            alertRepository.refreshAlerts()
        }

        // Observe alerts solely for lifecycle cleanup (silencing alarm / cancelling notification upon ACK/silence)
        alertRepository.getActiveAlerts()
            .onEach { alerts ->
                val currentIds = alerts.map { it.alertId }.toSet()

                // If an alert transitioned to SILENCED within active alerts
                alerts.forEach { alert ->
                    if (alert.status == AlertStatus.SILENCED) {
                        alarmController.silenceAlarm(alert.alertId)
                    }
                }

                // If an alert left active alerts (e.g. became ACKNOWLEDGED or EXPIRED)
                val removedIds = knownActiveAlertIds - currentIds
                removedIds.forEach { id ->
                    alarmController.silenceAlarm(id)
                    notificationManager.cancelAlertNotification(id)
                    knownActiveAlertIds.remove(id)
                }

                knownActiveAlertIds.addAll(currentIds)
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        alarmController.stopAlarm()
    }
}
