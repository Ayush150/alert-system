package com.sih26001.mobilealert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.AlertStatus
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel(
    private val notificationManager: AlertNotificationManager,
    private val alarmController: AlarmController,
    private val alertRepository: com.sih26001.mobilealert.domain.repository.AlertRepository = DependencyContainer.alertRepository
) : ViewModel() {

    private val activeAlertIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            alertRepository.refreshAlerts()
        }

        alertRepository.getActiveAlerts()
            .onEach { alerts ->
                val currentIds = alerts.map { it.alertId }.toSet()
                
                alerts.forEach { alert ->
                    // Handle new active alerts
                    if (alert.status == AlertStatus.ACTIVE && !activeAlertIds.contains(alert.alertId)) {
                        activeAlertIds.add(alert.alertId)
                        notificationManager.showAlertNotification(alert)
                        alarmController.startAlarm(alert)
                    }
                    
                    // Handle silenced/acknowledged alerts
                    if (alert.status != AlertStatus.ACTIVE) {
                        alarmController.silenceAlarm(alert.alertId)
                    }
                }

                // Handle removed alerts
                val removedIds = activeAlertIds - currentIds
                removedIds.forEach { id ->
                    notificationManager.cancelAlertNotification(id)
                    alarmController.silenceAlarm(id)
                    activeAlertIds.remove(id)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        alarmController.stopAlarm()
    }
}
