package com.sih26001.mobilealert.data.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SihFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "SihFirebaseMessaging"
        // Shared processor instance for deduplication across incoming messages
        val processor = FcmMessageProcessor()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Development-safe log confirming token refresh without exposing the full token
        val preview = if (token.length > 8) "${token.take(4)}...${token.takeLast(4)}" else "***"
        Log.i(TAG, "FCM token refreshed (length=${token.length}, preview=$preview)")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {
            val hasData = remoteMessage.data.isNotEmpty()
            val hasNotification = remoteMessage.notification != null
            Log.d(TAG, "FCM message received (hasData=$hasData, hasNotification=$hasNotification)")

            if (!hasData) {
                Log.w(TAG, "FCM message received without data payload")
                return
            }

            when (val result = processor.processData(remoteMessage.data)) {
                is FcmProcessingResult.ValidAlert -> {
                    Log.i(TAG, "FCM alert_id received: ${result.alertId}")
                    com.sih26001.mobilealert.di.DependencyContainer.fcmAlertTriggerHandler.handleAlertTrigger(result.alertId)
                }
                is FcmProcessingResult.MissingAlertId -> {
                    Log.w(TAG, "FCM message rejected: missing or empty alert_id")
                }
                is FcmProcessingResult.DuplicateAlert -> {
                    Log.d(TAG, "FCM duplicate message ignored: alert_id=${result.alertId}")
                }
                is FcmProcessingResult.EmptyPayload -> {
                    Log.w(TAG, "FCM message rejected: empty data payload")
                }
            }
        } catch (e: Exception) {
            // Never allow malformed or unexpected FCM messages to crash the application
            Log.e(TAG, "Error handling FCM message: ${e.message}", e)
        }
    }
}
