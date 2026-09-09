package com.sih26001.mobilealert.data.fcm

import android.util.Log
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.repository.AlertRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface FcmAlertTriggerHandler {
    fun handleAlertTrigger(alertId: String)
}

/**
 * Handles background orchestration of an FCM alert trigger.
 * Executes on an application-scoped CoroutineScope, fetches authoritative alert data from
 * the repository, persists to local storage, and conditionally triggers notifications and alarms.
 */
class FcmAlertTriggerHandlerImpl(
    private val alertRepository: AlertRepository,
    private val notificationManager: AlertNotificationManager,
    private val alarmController: AlarmController,
    private val coroutineScope: CoroutineScope
) : FcmAlertTriggerHandler {

    companion object {
        private const val TAG = "FcmAlertTriggerHandler"
    }

    override fun handleAlertTrigger(alertId: String) {
        coroutineScope.launch {
            Log.i(TAG, "Authoritative fetch started for alert_id=$alertId")
            val result = alertRepository.refreshAlert(alertId)

            result.onSuccess { alert ->
                Log.i(TAG, "Authoritative fetch succeeded for alert_id=$alertId")
                Log.i(TAG, "Alert persisted for alert_id=$alertId")

                if (alert.status != AlertStatus.EXPIRED) {
                    notificationManager.showAlertNotification(alert)

                    // Severity-based alarm condition: only HIGH and CRITICAL trigger audible/vibration alarm
                    if (alert.status == AlertStatus.ACTIVE &&
                        alert.severity in setOf(AlertSeverity.HIGH, AlertSeverity.CRITICAL)
                    ) {
                        alarmController.startAlarm(alert)
                    } else {
                        Log.d(TAG, "Alert $alertId is ${alert.severity}, skipping emergency alarm")
                    }
                } else {
                    Log.d(TAG, "Alert $alertId is expired, skipping notification and alarm")
                }
            }.onFailure { error ->
                Log.w(TAG, "Authoritative fetch failed for alert_id=$alertId: ${error.message}")
            }
        }
    }
}
