package com.sih26001.mobilealert.di

import com.sih26001.mobilealert.data.repository.MockAlertRepository
import com.sih26001.mobilealert.domain.repository.AlertRepository

/**
 * A simple manual dependency injection container for Phase 3.
 * Avoids adding external libraries while ensuring Singletons are shared.
 */
object DependencyContainer {
    
    // We use MockAlertRepository for Phase 3 to test the UI flow.
    val alertRepository: AlertRepository by lazy {
        MockAlertRepository()
    }

    // Explicitly expose as MockAlertRepository for developer tools 
    // since the UI needs to call triggerTestAlert() which is not in the interface.
    val mockAlertRepository: MockAlertRepository
        get() = alertRepository as MockAlertRepository
}
