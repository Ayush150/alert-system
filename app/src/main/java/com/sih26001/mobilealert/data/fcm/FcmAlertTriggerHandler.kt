package com.sih26001.mobilealert.data.fcm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.isDemoAlert
import com.sih26001.mobilealert.domain.repository.AlertRepository
import com.sih26001.mobilealert.presentation.emergency.EmergencyAlertActivity
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
    private val coroutineScope: CoroutineScope,
    private val context: Context? = null,
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L,
    private val backoffMultiplier: Double = 2.0
) : FcmAlertTriggerHandler {

    companion object {
        private const val TAG = "FcmAlertTriggerHandler"
    }

    override fun handleAlertTrigger(alertId: String) {
        coroutineScope.launch {
            var attempt = 0
            var currentDelayMs = initialDelayMs
            var lastError: Throwable? = null

            while (attempt <= maxRetries) {
                attempt++
                Log.i(TAG, "Authoritative fetch started for alert_id=$alertId (attempt $attempt/${maxRetries + 1})")
                val result = alertRepository.refreshAlert(alertId)

                if (result.isSuccess) {
                    val alert = result.getOrThrow()
                    Log.i(TAG, "Authoritative fetch succeeded for alert_id=$alertId on attempt $attempt")
                    Log.i(TAG, "Alert persisted for alert_id=$alertId")

                    if (alert.status != AlertStatus.EXPIRED) {
                        notificationManager.showAlertNotification(alert)

                        // Alarm activation is STRICTLY gated:
                        // 1. Alert was delivered through authoritative backend/FCM path (handled here in FcmAlertTriggerHandler).
                        // 2. Alert must explicitly qualify as an active system-generated demo alert (alert.isDemoAlert()).
                        // 3. Severity must be HIGH or CRITICAL (NORMAL demo alerts do NOT alarm).
                        // 4. Real cloud alerts (Risk Engine, IMD, Geological Survey, etc.) MUST NEVER trigger physical hardware alarms.
                        if (alert.status == AlertStatus.ACTIVE &&
                            alert.severity in setOf(AlertSeverity.HIGH, AlertSeverity.CRITICAL) &&
                            alert.isDemoAlert()
                        ) {
                            alarmController.startAlarm(alert)
                            launchEmergencyActivity(alert.alertId)
                        } else {
                            Log.d(TAG, "Alert $alertId is not an active demo HIGH/CRITICAL alert; skipping physical alarm")
                        }
                    } else {
                        Log.d(TAG, "Alert $alertId is expired, skipping notification and alarm")
                    }
                    return@launch
                }

                val error = result.exceptionOrNull()
                lastError = error

                // Non-recoverable validation failures must abort immediately without wasting retries
                if (error is IllegalArgumentException) {
                    Log.w(TAG, "Authoritative fetch rejected for alert_id=$alertId due to validation error: ${error.message}. Aborting retries.")
                    return@launch
                }

                if (attempt <= maxRetries) {
                    Log.w(TAG, "Authoritative fetch attempt $attempt failed for alert_id=$alertId: ${error?.message}. Retrying in ${currentDelayMs}ms...")
                    kotlinx.coroutines.delay(currentDelayMs)
                    currentDelayMs = (currentDelayMs * backoffMultiplier).toLong()
                }
            }

            Log.w(TAG, "Authoritative fetch permanently failed after ${maxRetries + 1} attempts for alert_id=$alertId: ${lastError?.message}")
        }
    }

    private fun launchEmergencyActivity(alertId: String) {
        val targetContext = context ?: try {
            DependencyContainer.appContext
        } catch (_: Throwable) {
            null
        }

        if (targetContext != null) {
            try {
                val intent = Intent(targetContext, EmergencyAlertActivity::class.java).apply {
                    putExtra("alert_id", alertId)
                    data = Uri.parse("sih26001://emergency/$alertId")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                targetContext.startActivity(intent)
                Log.i(TAG, "Launched EmergencyAlertActivity for alert_id=$alertId")
            } catch (e: Exception) {
                Log.w(TAG, "Direct launch of EmergencyAlertActivity failed; fullScreenIntent will handle wake/presentation: ${e.message}")
            }
        }
    }
}
