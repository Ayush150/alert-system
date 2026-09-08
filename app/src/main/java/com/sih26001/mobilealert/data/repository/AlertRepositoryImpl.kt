package com.sih26001.mobilealert.data.repository

import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Phase 1/2 placeholder repository implementation.
 *
 * NOTE:
 * - Empty collections are safely emitted so empty states are verified without fabricating mock data.
 * - Phase 3 will introduce mock alerts and sample triggers.
 * - Later phases will connect Room persistence and remote REST/FCM synchronization.
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

    override suspend fun acknowledgeAlert(alertId: String): Result<Unit> {
        // Operational sync to backend will be implemented in subsequent phases.
        return Result.success(Unit)
    }

    override suspend fun silenceAlert(alertId: String): Result<Unit> {
        // Local audio/vibrator suppression will be implemented in notification phase.
        return Result.success(Unit)
    }
}
