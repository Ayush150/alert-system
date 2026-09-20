package com.sih26001.mobilealert.data.fcm

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.sih26001.mobilealert.core.util.Constants

/**
 * Handles Firebase Cloud Messaging topic subscription for SIH26001 emergency alert broadcasts.
 *
 * Rules:
 * - Subscribes to authoritative emergency alert topic: "sih26001_alerts".
 * - Never logs or exposes device registration tokens.
 * - Logs strictly safe success / failure diagnostics.
 * - Does not require or request location or sensitive permissions.
 */
object FcmTopicSubscriber {

    private const val TAG = "FcmTopicSubscriber"
    const val TOPIC_ALERTS = Constants.FCM_TOPIC_ALERTS

    /**
     * Subscribes the device to the authoritative FCM alert topic.
     *
     * @param messaging FirebaseMessaging instance (defaults to FirebaseMessaging.getInstance()).
     * @param topic Topic name (defaults to "sih26001_alerts").
     * @param onComplete Optional completion listener for verification and unit testing.
     * @return The underlying Google Play Services Task, or null if initialization failed.
     */
    fun subscribeToAlertsTopic(
        messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
        topic: String = TOPIC_ALERTS,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ): Task<Void>? {
        return try {
            messaging.subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "FCM topic subscription successful: $topic")
                        onComplete?.invoke(true, null)
                    } else {
                        val errorMsg = task.exception?.message ?: "Unknown error"
                        Log.w(TAG, "FCM topic subscription failed: $errorMsg")
                        onComplete?.invoke(false, errorMsg)
                    }
                }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Exception during topic subscription"
            Log.e(TAG, "FCM topic subscription failed: $errorMsg")
            onComplete?.invoke(false, errorMsg)
            null
        }
    }
}
