package com.sih26001.mobilealert.domain.repository

import com.sih26001.mobilealert.domain.model.Alert
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository abstraction for accessing and modifying alert state.
 *
 * Designed to support future implementations (Mock, Room, REST, FCM-triggered)
 * without altering domain or UI layer consumers.
 */
interface AlertRepository {
    /**
     * Emits a reactive stream of currently active alerts requiring user attention.
     */
    fun getActiveAlerts(): Flow<List<Alert>>

    /**
     * Emits a reactive stream of past, acknowledged, or expired alerts.
     */
    fun getAlertHistory(): Flow<List<Alert>>

    /**
     * Observes an individual alert by its unique [alertId].
     */
    fun getAlertById(alertId: String): Flow<Alert?>

    /**
     * Marks an alert as acknowledged.
     * This is an operational confirmation that must eventually sync to the backend.
     */
    suspend fun acknowledgeAlert(alertId: String): Result<Unit>

    /**
     * Suppresses local audible or vibration alarms for an active alert.
     * NOTE: Silencing is a local UX action and does NOT acknowledge the alert.
     */
    suspend fun silenceAlert(alertId: String): Result<Unit>

    /**
     * Triggers a manual network fetch of alerts from the remote source.
     */
    suspend fun refreshAlerts(): Result<Unit>

    /**
     * Fetches and refreshes an authoritative alert by its unique [alertId] from the remote source,
     * validating it and persisting it to local storage.
     */
    suspend fun refreshAlert(alertId: String): Result<Alert>
}
