package com.sih26001.mobilealert.data.ack

import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.PendingAckEntity
import java.time.Instant

/**
 * Defines the recovery semantics for pending acknowledgements that were interrupted
 * (e.g., due to process death) while IN_FLIGHT.
 */
interface AckRecoveryPolicy {
    /**
     * Determines whether an IN_FLIGHT [pendingAck] is considered stale and should be
     * safely transitioned to a retryable state.
     *
     * @param pendingAck The entity to evaluate.
     * @param currentTime The current time used to calculate staleness.
     * @return true if the record is IN_FLIGHT and stale, false otherwise.
     */
    fun isStale(pendingAck: PendingAckEntity, currentTime: Instant): Boolean
}

/**
 * Default implementation using a conservative time-based threshold.
 *
 * @param staleThresholdMs The amount of time in milliseconds an IN_FLIGHT record
 * must remain untouched before it is considered stale. Defaults to 5 minutes (300,000 ms).
 */
class DefaultAckRecoveryPolicy(
    private val staleThresholdMs: Long = 300_000L
) : AckRecoveryPolicy {

    override fun isStale(pendingAck: PendingAckEntity, currentTime: Instant): Boolean {
        // Only IN_FLIGHT records can be considered for stale recovery
        if (pendingAck.status != AckSyncStatus.IN_FLIGHT) {
            return false
        }

        val lastAttempt = pendingAck.lastAttemptAt
        if (lastAttempt == null) {
            // If it's IN_FLIGHT but has no lastAttemptAt timestamp, it's an anomalous state.
            // Conservatively treat it as stale so it can recover.
            return true
        }

        val staleTime = lastAttempt.plusMillis(staleThresholdMs)
        return !currentTime.isBefore(staleTime)
    }
}
