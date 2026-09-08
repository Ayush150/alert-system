package com.sih26001.mobilealert.data.repository

import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Phase 1 placeholder repository implementation.
 *
 * NOTE:
 * - Real backend synchronization, push notification ingestion, and Room persistence
 *   will be introduced in subsequent phases.
 * - In Phase 1, empty collections are safely emitted so empty states are verified
 *   without fabricating mock data.
 */
class AlertRepositoryImpl : AlertRepository {

    override fun getActiveAlerts(): Flow<List<Alert>> {
        return flowOf(emptyList())
    }

    override fun getAlertHistory(): Flow<List<Alert>> {
        return flowOf(emptyList())
    }

    override fun getAlertById(alertId: String): Flow<Alert?> {
        return flowOf(null)
    }

    override suspend fun acknowledgeAlert(alertId: String) {
        // Operational sync to backend will be implemented in subsequent phases.
    }

    override suspend fun silenceAlert(alertId: String) {
        // Local audio/vibrator suppression will be implemented in notification phase.
    }
}
