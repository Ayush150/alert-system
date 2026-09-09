package com.sih26001.mobilealert.data.ack

import android.util.Log
import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.PendingAckDao
import com.sih26001.mobilealert.data.local.PendingAckEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Application-scoped synchronization engine for operational acknowledgements.
 *
 * Coordinates reading pending ACK records from Room, evaluating backoff policy,
 * executing the state machine transitions (PENDING -> IN_FLIGHT -> COMPLETED / FAILED),
 * preventing duplicate concurrent transport attempts for the same alert, and gracefully
 * handling transport unavailability or errors.
 */
class AckSyncEngine(
    private val pendingAckDao: PendingAckDao,
    private val ackSyncDataSource: AckSyncDataSource,
    private val ackRetryPolicy: AckRetryPolicy,
    private val ackRecoveryPolicy: AckRecoveryPolicy,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "AckSyncEngine"
    }

    // Guards access to the set of alerts currently in flight to prevent concurrent sync attempts
    private val lock = Mutex()
    private val inFlightAlerts = mutableSetOf<String>()

    /**
     * Inspects the database for IN_FLIGHT records that are stale (due to process death).
     * Transitions them to FAILED so they can be picked up by the normal retry backoff.
     *
     * @return Number of recovered records.
     */
    suspend fun recoverInterruptedSyncs(): Int = withContext(dispatcher) {
        val allPending = pendingAckDao.getPendingAcks()
        val inFlightRecords = allPending.filter { it.status == AckSyncStatus.IN_FLIGHT }
        
        if (inFlightRecords.isEmpty()) {
            return@withContext 0
        }

        val now = Instant.now()
        var recoveredCount = 0

        for (ack in inFlightRecords) {
            if (ackRecoveryPolicy.isStale(ack, now)) {
                try {
                    // Transition: IN_FLIGHT -> FAILED (stale recovery)
                    // Preserve retryCount and acknowledgedAt.
                    val recoveredEntity = ack.copy(
                        status = AckSyncStatus.FAILED,
                        // We do not increment retryCount here. The actual retry attempt
                        // should increment the count if it fails.
                        lastAttemptAt = now // Updating lastAttemptAt so backoff applies from now
                    )
                    pendingAckDao.update(recoveredEntity)
                    recoveredCount++
                    Log.i(TAG, "Recovered stale IN_FLIGHT record for alert=${ack.alertId} to FAILED state.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to recover stale record for alert=${ack.alertId}", e)
                }
            }
        }
        return@withContext recoveredCount
    }

    /**
     * Inspects Room for pending/failed operational acknowledgements, verifies eligibility
     * against network state and retry backoff, and attempts synchronization.
     *
     * @return Number of records attempted during this reconciliation cycle.
     */
    suspend fun reconcilePendingAcks(): Int = withContext(dispatcher) {
        if (!connectivityMonitor.isOnline()) {
            Log.d(TAG, "Reconciliation skipped: device is currently offline")
            return@withContext 0
        }

        val eligibleAcks = pendingAckDao.getEligiblePendingAcks()
        if (eligibleAcks.isEmpty()) {
            Log.d(TAG, "Reconciliation: no eligible pending ACKs found")
            return@withContext 0
        }

        val now = Instant.now()
        var attemptedCount = 0

        for (ack in eligibleAcks) {
            // Ignore COMPLETED records defensively
            if (ack.status == AckSyncStatus.COMPLETED) {
                continue
            }

            // Check retry eligibility
            if (!ackRetryPolicy.shouldRetry(ack, now)) {
                Log.d(TAG, "Alert ${ack.alertId} not eligible for retry yet (retryCount=${ack.retryCount}, status=${ack.status})")
                continue
            }

            // Try to acquire in-flight lock for this alert
            val acquired = lock.withLock {
                if (inFlightAlerts.contains(ack.alertId)) {
                    false
                } else {
                    inFlightAlerts.add(ack.alertId)
                    true
                }
            }

            if (!acquired) {
                Log.d(TAG, "Alert ${ack.alertId} is already in-flight, skipping concurrent attempt")
                continue
            }

            attemptedCount++

            try {
                // Re-fetch the entity in case it was modified since we queried eligibleAcks
                val currentAck = pendingAckDao.getPendingAckById(ack.alertId)
                if (currentAck == null || currentAck.status == AckSyncStatus.COMPLETED || currentAck.status == AckSyncStatus.IN_FLIGHT) {
                    // State changed while waiting to process
                    Log.d(TAG, "Alert ${ack.alertId} state changed before processing, skipping.")
                    continue
                }

                // Transition: -> IN_FLIGHT
                val attemptTime = Instant.now()
                val inFlightEntity = currentAck.copy(
                    status = AckSyncStatus.IN_FLIGHT,
                    lastAttemptAt = attemptTime
                )
                pendingAckDao.update(inFlightEntity)
                Log.i(TAG, "Sync attempt started for alert=${currentAck.alertId}, attempt=${currentAck.retryCount + 1}")

                // Invoke contract transport
                val result = ackSyncDataSource.synchronizeAck(inFlightEntity)

                if (result.isSuccess) {
                    // Transition: -> COMPLETED on genuine authoritative server success
                    val completedEntity = inFlightEntity.copy(
                        status = AckSyncStatus.COMPLETED
                    )
                    pendingAckDao.update(completedEntity)
                    Log.i(TAG, "Sync succeeded for alert=${currentAck.alertId}: marked COMPLETED")
                } else {
                    // Transition: -> FAILED
                    val failureThrowable = result.exceptionOrNull()
                    val failedEntity = inFlightEntity.copy(
                        status = AckSyncStatus.FAILED,
                        retryCount = currentAck.retryCount + 1
                    )
                    pendingAckDao.update(failedEntity)
                    Log.w(TAG, "Sync failed for alert=${currentAck.alertId} (retryCount=${failedEntity.retryCount}): ${failureThrowable?.message}")
                }
            } catch (t: Throwable) {
                // Catch any unexpected exception to prevent crashing the engine or process
                Log.e(TAG, "Unexpected error during ACK sync for alert=${ack.alertId}", t)
                try {
                    val failedEntity = ack.copy(
                        status = AckSyncStatus.FAILED,
                        retryCount = ack.retryCount + 1,
                        lastAttemptAt = Instant.now()
                    )
                    pendingAckDao.update(failedEntity)
                } catch (dbEx: Exception) {
                    Log.e(TAG, "Failed to record FAILED status in Room for alert=${ack.alertId}", dbEx)
                }
            } finally {
                lock.withLock {
                    inFlightAlerts.remove(ack.alertId)
                }
            }
        }

        return@withContext attemptedCount
    }
}
