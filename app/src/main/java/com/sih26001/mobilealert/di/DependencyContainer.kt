package com.sih26001.mobilealert.di

import com.sih26001.mobilealert.data.repository.MockAlertRepository
import com.sih26001.mobilealert.domain.repository.AlertRepository

/**
 * A simple manual dependency injection container for Phase 3.
 * Avoids adding external libraries while ensuring Singletons are shared.
 */
object DependencyContainer {
    
    lateinit var appContext: android.content.Context
        private set

    lateinit var database: com.sih26001.mobilealert.data.local.AppDatabase
        private set

    val applicationScope: kotlinx.coroutines.CoroutineScope by lazy {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    }

    fun initialize(context: android.content.Context) {
        appContext = context.applicationContext
        database = androidx.room.Room.databaseBuilder(
            appContext,
            com.sih26001.mobilealert.data.local.AppDatabase::class.java,
            "alerts.db"
        ).build()
    }

    // We use AlertRepositoryImpl for Phase 6 to connect to the local Mock REST API and Room.
    val alertRepository: AlertRepository by lazy {
        com.sih26001.mobilealert.data.repository.AlertRepositoryImpl(
            alertApiService, 
            database.alertDao()
        )
    }

    // Explicitly expose MockAlertRepository separately for developer tools
    // so tests and developer buttons don't crash.
    val mockAlertRepository: MockAlertRepository by lazy {
        MockAlertRepository()
    }

    // Expose the Retrofit API service for future repository integration
    val alertApiService: com.sih26001.mobilealert.data.remote.api.AlertApiService by lazy {
        com.sih26001.mobilealert.data.remote.RetrofitProvider.provideAlertApiService()
    }

    // Expose the FCM trigger handler for background authoritative processing
    val fcmAlertTriggerHandler: com.sih26001.mobilealert.data.fcm.FcmAlertTriggerHandler by lazy {
        com.sih26001.mobilealert.data.fcm.FcmAlertTriggerHandlerImpl(
            alertRepository = alertRepository,
            notificationManager = com.sih26001.mobilealert.core.notification.AlertNotificationManagerImpl(appContext),
            alarmController = com.sih26001.mobilealert.core.alarm.AlarmControllerImpl(appContext),
            coroutineScope = applicationScope
        )
    }
}
