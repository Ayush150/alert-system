package com.sih26001.mobilealert.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.sih26001.mobilealert.R
import com.sih26001.mobilealert.core.util.Constants
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity

interface AlertNotificationManager {
    fun showAlertNotification(alert: Alert)
    fun cancelAlertNotification(alertId: String)
}

class AlertNotificationManagerImpl(private val context: Context) : AlertNotificationManager {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun showAlertNotification(alert: Alert) {
        val channelId = when (alert.severity) {
            AlertSeverity.NORMAL -> Constants.CHANNEL_ID_NORMAL
            AlertSeverity.HIGH -> Constants.CHANNEL_ID_HIGH
            AlertSeverity.CRITICAL -> Constants.CHANNEL_ID_CRITICAL
        }

        // Create deep link intent to ActiveAlarmScreen
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sih26001://alert/${alert.alertId}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.alertId.hashCode(), // Deterministic request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val riskText = alert.riskScore?.let { score ->
            "Risk score: ${(score * 100).toInt()}/100"
        } ?: "Risk score unavailable"

        val contentText = buildString {
            if (alert.location != null) {
                append("Location: ${alert.location.latitude}, ${alert.location.longitude}\n")
            }
            append(riskText)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // using android system icon as placeholder
            .setContentTitle("${alert.severity.name} ${alert.eventType}")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${contentText}\n\n${alert.recommendedAction ?: ""}"))
            .setPriority(when (alert.severity) {
                AlertSeverity.CRITICAL -> NotificationCompat.PRIORITY_MAX
                AlertSeverity.HIGH -> NotificationCompat.PRIORITY_HIGH
                else -> NotificationCompat.PRIORITY_DEFAULT
            })
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (alert.severity == AlertSeverity.CRITICAL) {
            builder.setFullScreenIntent(pendingIntent, true)
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        }

        // Use alertId hash as deterministic notification ID
        notificationManager.notify(alert.alertId.hashCode(), builder.build())
    }

    override fun cancelAlertNotification(alertId: String) {
        notificationManager.cancel(alertId.hashCode())
    }
}
