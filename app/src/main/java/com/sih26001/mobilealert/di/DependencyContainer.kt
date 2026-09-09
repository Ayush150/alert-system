package com.sih26001.mobilealert.di

import com.sih26001.mobilealert.data.repository.MockAlertRepository
import com.sih26001.mobilealert.domain.repository.AlertRepository

/**
 * A simple manual dependency injection container for Phase 3.
 * Avoids adding external libraries while ensuring Singletons are shared.
 */
object DependencyContainer {
    
    lateinit var database: com.sih26001.mobilealert.data.local.AppDatabase
        private set

    fun initialize(context: android.content.Context) {
        database = androidx.room.Room.databaseBuilder(
            context.applicationContext,
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
}
