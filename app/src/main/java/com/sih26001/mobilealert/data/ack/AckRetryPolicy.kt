package com.sih26001.mobilealert.data.ack

import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.PendingAckEntity
import java.time.Instant

/**
 * Reusable retry and backoff policy abstraction for ACK synchronization.
 */
interface AckRetryPolicy {
    /**
     * Determines whether [pendingAck] is currently eligible for a synchronization attempt
     * based on its status, attempt count, and last attempt time compared to [currentTime].
     */
    fun shouldRetry(
        pendingAck: PendingAckEntity,
        currentTime: Instant
    ): Boolean

    /**
     * Calculates the minimum backoff delay in milliseconds to wait before the next attempt,
     * given the [retryCount].
     */
    fun nextRetryDelayMs(
        retryCount: Int
    ): Long
}

/**
 * Default exponential backoff policy for ACK synchronization.
 *
 * Parameters:
 * - base delay: 2,000 ms (2 seconds)
 * - multiplier: 2.0
 * - max delay: 300,000 ms (5 minutes)
 * - max retries: 10
 */
class ExponentialBackoffAckRetryPolicy(
    private val baseDelayMs: Long = 2_000L,
    private val multiplier: Double = 2.0,
    private val maxDelayMs: Long = 300_000L, // 5 minutes
    private val maxRetries: Int = 10
) : AckRetryPolicy {

    override fun nextRetryDelayMs(retryCount: Int): Long {
        if (retryCount <= 0) return baseDelayMs
        
        // Use overflow-safe arithmetic.
        // We only care up to maxRetries anyway, but let's be absolutely safe.
        // maxRetries is usually 10. multiplier is 2.0.
        // 2.0 ^ 10 = 1024. 2000 * 1024 = ~2,000,000 which easily fits in Long.
        // For extremely large retryCount, we cap it early to prevent Double overflow.
        val safeRetryCount = retryCount.coerceAtMost(30) // 2^30 is safe, avoids Double.POSITIVE_INFINITY
        
        val factor = Math.pow(multiplier, safeRetryCount.toDouble())
        
        // Prevent overflow during multiplication
        val maxFactor = Long.MAX_VALUE / baseDelayMs.coerceAtLeast(1)
        val calculated = if (factor > maxFactor) {
            Long.MAX_VALUE
        } else {
            (baseDelayMs * factor).toLong()
        }
        
        return calculated.coerceAtMost(maxDelayMs)
    }

    override fun shouldRetry(
        pendingAck: PendingAckEntity,
        currentTime: Instant
    ): Boolean {
        // COMPLETED records must never be retried
        if (pendingAck.status == AckSyncStatus.COMPLETED) {
            return false
        }

        // IN_FLIGHT records are actively being processed or need recovery, not normal retry
        if (pendingAck.status == AckSyncStatus.IN_FLIGHT) {
            return false
        }

        // Exceeded max retries: do not retry automatically
        if (pendingAck.retryCount >= maxRetries) {
            return false
        }

        // PENDING records are immediately eligible for their first attempt
        if (pendingAck.status == AckSyncStatus.PENDING) {
            return true
        }

        // FAILED records must wait until the backoff delay has elapsed since lastAttemptAt
        if (pendingAck.status == AckSyncStatus.FAILED) {
            val lastAttempt = pendingAck.lastAttemptAt
            if (lastAttempt == null) {
                // If lastAttemptAt is null but status is FAILED, something anomalous happened.
                // Safely permit retry to unstuck it.
                return true
            }
            val delayMs = nextRetryDelayMs(pendingAck.retryCount)
            val nextEligibleTime = lastAttempt.plusMillis(delayMs)
            return !currentTime.isBefore(nextEligibleTime)
        }

        return false
    }
}
