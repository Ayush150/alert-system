package com.sih26001.mobilealert.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.sih26001.mobilealert.core.util.Constants

object NotificationChannels {

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // NORMAL channel
            val normalChannel = NotificationChannel(
                Constants.CHANNEL_ID_NORMAL,
                "Normal Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standard weather warnings and updates"
            }

            // HIGH channel
            val highChannel = NotificationChannel(
                Constants.CHANNEL_ID_HIGH,
                "High Priority Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority warnings requiring attention"
                enableVibration(true)
            }

            // CRITICAL channel
            val criticalChannel = NotificationChannel(
                Constants.CHANNEL_ID_CRITICAL,
                "Critical Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH // MAX is deprecated, HIGH is appropriate
            ).apply {
                description = "Critical life-safety emergency warnings"
                enableVibration(true)
                setBypassDnd(true) // Attempt to bypass Do Not Disturb for critical
            }

            notificationManager.createNotificationChannels(listOf(normalChannel, highChannel, criticalChannel))
        }
    }
}
