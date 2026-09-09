package com.sih26001.mobilealert.di

import com.sih26001.mobilealert.data.repository.MockAlertRepository
import com.sih26001.mobilealert.domain.repository.AlertRepository
import androidx.room.withTransaction
import android.util.Log
import kotlinx.coroutines.launch

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
        ).fallbackToDestructiveMigration().build()

        // Start observing network changes
        ackSyncCoordinator.startObserving()

        // Trigger startup recovery and reconciliation
        applicationScope.launch {
            try {
                Log.d("DependencyContainer", "Executing startup ACK recovery")
                val recovered = ackSyncEngine.recoverInterruptedSyncs()
                if (recovered > 0) {
                    Log.i("DependencyContainer", "Recovered $recovered stale IN_FLIGHT records on startup.")
                }
                val processed = ackSyncEngine.reconcilePendingAcks()
                if (processed > 0) {
                    Log.i("DependencyContainer", "Reconciled $processed ACK records on startup.")
                }
            } catch (t: Throwable) {
                Log.e("DependencyContainer", "Unexpected error during startup ACK recovery/sync", t)
            }
        }
    }

    val databaseTransactionRunner: com.sih26001.mobilealert.data.local.DatabaseTransactionRunner by lazy {
        object : com.sih26001.mobilealert.data.local.DatabaseTransactionRunner {
            override suspend fun <T> invoke(block: suspend () -> T): T {
                return database.withTransaction {
                    block()
                }
            }
        }
    }

    // We use AlertRepositoryImpl for Phase 6 to connect to the local Mock REST API and Room.
    val alertRepository: AlertRepository by lazy {
        com.sih26001.mobilealert.data.repository.AlertRepositoryImpl(
            apiService = alertApiService, 
            alertDao = database.alertDao(),
            pendingAckDao = database.pendingAckDao(),
            transactionRunner = databaseTransactionRunner
        )
    }

    val acknowledgeAlertUseCase: com.sih26001.mobilealert.domain.usecase.AcknowledgeAlertUseCase by lazy {
        com.sih26001.mobilealert.domain.usecase.AcknowledgeAlertUseCase(alertRepository)
    }

    val pendingAckDao: com.sih26001.mobilealert.data.local.PendingAckDao by lazy {
        database.pendingAckDao()
    }

    val networkConnectivityMonitor: com.sih26001.mobilealert.data.ack.NetworkConnectivityMonitor by lazy {
        com.sih26001.mobilealert.data.ack.AndroidNetworkConnectivityMonitor(appContext)
    }

    val ackRetryPolicy: com.sih26001.mobilealert.data.ack.AckRetryPolicy by lazy {
        com.sih26001.mobilealert.data.ack.ExponentialBackoffAckRetryPolicy()
    }

    val ackRecoveryPolicy: com.sih26001.mobilealert.data.ack.AckRecoveryPolicy by lazy {
        com.sih26001.mobilealert.data.ack.DefaultAckRecoveryPolicy()
    }

    // Production transport MUST remain unavailable and MUST NEVER report fake success
    val ackSyncDataSource: com.sih26001.mobilealert.data.ack.AckSyncDataSource by lazy {
        com.sih26001.mobilealert.data.ack.UnavailableAckSyncDataSource()
    }

    val ackSyncEngine: com.sih26001.mobilealert.data.ack.AckSyncEngine by lazy {
        com.sih26001.mobilealert.data.ack.AckSyncEngine(
            pendingAckDao = pendingAckDao,
            ackSyncDataSource = ackSyncDataSource,
            ackRetryPolicy = ackRetryPolicy,
            ackRecoveryPolicy = ackRecoveryPolicy,
            connectivityMonitor = networkConnectivityMonitor
        )
    }

    val ackSyncCoordinator: com.sih26001.mobilealert.data.ack.AckSyncCoordinator by lazy {
        com.sih26001.mobilealert.data.ack.AckSyncCoordinator(
            context = appContext,
            engine = ackSyncEngine,
            scope = applicationScope
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
