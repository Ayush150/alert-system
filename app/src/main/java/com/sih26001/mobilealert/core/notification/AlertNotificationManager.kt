package com.sih26001.mobilealert.core.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sih26001.mobilealert.R
import com.sih26001.mobilealert.core.util.Constants
import com.sih26001.mobilealert.core.util.LocaleManager
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.Location
import com.sih26001.mobilealert.domain.model.isDemoAlert
import java.util.Locale

interface AlertNotificationManager {
    fun showAlertNotification(alert: Alert)
    fun cancelAlertNotification(alertId: String)
}

class AlertNotificationManagerImpl(
    private val context: Context,
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
) : AlertNotificationManager {

    companion object {
        private const val TAG = "AlertNotificationManager"

        fun getChannelIdForSeverity(severity: AlertSeverity): String = when (severity) {
            AlertSeverity.NORMAL -> Constants.CHANNEL_ID_NORMAL
            AlertSeverity.HIGH -> Constants.CHANNEL_ID_HIGH
            AlertSeverity.CRITICAL -> Constants.CHANNEL_ID_CRITICAL
        }

        fun getPriorityForSeverity(severity: AlertSeverity): Int = when (severity) {
            AlertSeverity.CRITICAL -> NotificationCompat.PRIORITY_MAX
            AlertSeverity.HIGH -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        fun formatRiskScore(riskScore: Double?): String {
            return riskScore?.let { score ->
                "Risk Score: ${String.format(Locale.US, "%.1f%%", score)}"
            } ?: "Risk score unavailable"
        }

        fun formatLocation(location: Location?): String? {
            if (location == null) return null
            val name = location.name?.trim()?.takeIf { it.isNotEmpty() }
            val hasCoords = location.latitude != null && location.longitude != null

            return when {
                name != null && hasCoords ->
                    "Location: $name\nCoordinates: ${location.latitude}, ${location.longitude}"
                name != null ->
                    "Location: $name"
                hasCoords ->
                    "Location: ${location.latitude}, ${location.longitude}"
                else -> null
            }
        }

        fun formatContentText(alert: Alert): String {
            val locationText = formatLocation(alert.location)
            val riskText = formatRiskScore(alert.riskScore)

            return buildString {
                if (!locationText.isNullOrBlank()) {
                    append(locationText)
                    append("\n")
                }
                append(riskText)
            }
        }

        fun getDeepLinkUriString(alertId: String): String {
            return "sih26001://alert/$alertId"
        }

        fun getDeepLinkUri(alertId: String): Uri {
            return Uri.parse(getDeepLinkUriString(alertId))
        }

        fun getNotificationId(alertId: String): Int {
            return alertId.hashCode()
        }

        fun createDeepLinkIntent(alertId: String): Intent {
            return Intent(Intent.ACTION_VIEW, getDeepLinkUri(alertId)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }

        fun createEmergencyIntent(context: Context, alertId: String): Intent {
            return Intent(context, com.sih26001.mobilealert.presentation.emergency.EmergencyAlertActivity::class.java).apply {
                putExtra("alert_id", alertId)
                data = Uri.parse("sih26001://emergency/$alertId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
    }

    override fun showAlertNotification(alert: Alert) {
        val channelId = getChannelIdForSeverity(alert.severity)

        // Defensive channel existence check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (notificationManager.getNotificationChannel(channelId) == null) {
                    Log.w(TAG, "Notification channel $channelId does not exist. Re-initializing channels...")
                    NotificationChannels.createChannels(context)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Defensive channel check failed for channel $channelId: ${e.message}")
            }
        }

        val notification = buildNotification(alert, channelId)
        val notificationId = getNotificationId(alert.alertId)

        // Defensive notification posting
        try {
            notificationManager.notify(notificationId, notification)
            Log.i(TAG, "Posted notification for alert_id=${alert.alertId} (id=$notificationId) on channel=$channelId")
        } catch (e: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS permission denied or revoked; cannot post notification for alert_id=${alert.alertId}: ${e.message}")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to post notification for alert_id=${alert.alertId}: ${e.message}", e)
        }
    }

    override fun cancelAlertNotification(alertId: String) {
        val notificationId = getNotificationId(alertId)
        try {
            notificationManager.cancel(notificationId)
            Log.i(TAG, "Cancelled notification for alert_id=$alertId (id=$notificationId)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel notification for alert_id=$alertId: ${e.message}")
        }
    }

    fun buildNotification(alert: Alert, channelId: String = getChannelIdForSeverity(alert.severity)): Notification {
        val localizedContext = try {
            LocaleManager.getLocalizedContext(context)
        } catch (_: Exception) {
            context
        }

        val isEmergencyDemoAlert = alert.isDemoAlert() && 
            alert.severity in setOf(AlertSeverity.HIGH, AlertSeverity.CRITICAL)

        val pendingIntent = if (isEmergencyDemoAlert) {
            createEmergencyPendingIntent(alert.alertId)
        } else {
            createPendingIntent(alert.alertId)
        }

        val title: String
        val contentText: String

        if (isEmergencyDemoAlert) {
            val severityLabel = when (alert.severity) {
                AlertSeverity.CRITICAL -> localizedContext.getString(R.string.alert_critical_title)
                AlertSeverity.HIGH -> localizedContext.getString(R.string.alert_high_title)
                AlertSeverity.NORMAL -> localizedContext.getString(R.string.alert_warning_title)
            }
            val hazardLabel = localizedContext.getString(R.string.hazard_landslide_detected)
            title = localizedContext.getString(R.string.notification_emergency_title, severityLabel, hazardLabel)

            val areaName = alert.location?.name ?: localizedContext.getString(R.string.default_location)
            contentText = localizedContext.getString(R.string.notification_emergency_body, areaName)
        } else {
            title = "${alert.severity.name} ${alert.eventType}"
            contentText = formatContentText(alert)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(getPriorityForSeverity(alert.severity))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Wake screen and launch full-screen emergency intent for demo HIGH/CRITICAL or any CRITICAL alert
        if (isEmergencyDemoAlert || alert.severity == AlertSeverity.CRITICAL) {
            builder.setFullScreenIntent(pendingIntent, true)
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
        }

        return builder.build()
    }

    private fun createPendingIntent(alertId: String): PendingIntent {
        val intent = createDeepLinkIntent(alertId)
        return PendingIntent.getActivity(
            context,
            getNotificationId(alertId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createEmergencyPendingIntent(alertId: String): PendingIntent {
        val intent = createEmergencyIntent(context, alertId)
        return PendingIntent.getActivity(
            context,
            getNotificationId(alertId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
