package com.sih26001.mobilealert.domain.repository

import com.sih26001.mobilealert.domain.model.Alert
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing alerts.
 * Concrete implementations reside in the data layer.
 */
interface AlertRepository {
    /**
     * Observes currently active alerts that require user attention.
     */
    fun getActiveAlerts(): Flow<List<Alert>>

    /**
     * Observes the historical record of alerts (acknowledged, expired, past).
     */
    fun getAlertHistory(): Flow<List<Alert>>

    /**
     * Observes an individual alert by its unique alert_id.
     */
    fun getAlertById(alertId: String): Flow<Alert?>

    /**
     * Acknowledges an alert. This is an operational event that must eventually sync to backend.
     */
    suspend fun acknowledgeAlert(alertId: String)

    /**
     * Silences local audible/tactile warnings for the given alert.
     * Note: Silencing is a local UX action and does NOT mark the alert as acknowledged.
     */
    suspend fun silenceAlert(alertId: String)
}
