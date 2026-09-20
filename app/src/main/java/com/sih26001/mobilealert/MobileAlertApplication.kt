package com.sih26001.mobilealert

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.sih26001.mobilealert.core.notification.NotificationChannels
import com.sih26001.mobilealert.data.fcm.FcmTopicSubscriber
import com.sih26001.mobilealert.di.DependencyContainer

class MobileAlertApplication : Application() {

    companion object {
        private const val TAG = "MobileAlertApplication"
    }

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        DependencyContainer.initialize(this)
        com.sih26001.mobilealert.core.util.LocaleManager.init(this)
        NotificationChannels.createChannels(this)

        // Subscribe to authoritative emergency alerts topic on application startup
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            FcmTopicSubscriber.subscribeToAlertsTopic()
        } else {
            Log.w(TAG, "Firebase not initialized. Skipping FCM topic subscription.")
        }
    }
}


