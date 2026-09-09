package com.sih26001.mobilealert.data.ack

import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.PendingAckEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AckRetryPolicyTest {

    private val policy = ExponentialBackoffAckRetryPolicy(
        baseDelayMs = 2_000L,
        multiplier = 2.0,
        maxDelayMs = 300_000L, // 5 minutes
        maxRetries = 10
    )

    @Test
    fun `retry 0 delay is base delay`() {
        assertEquals(2_000L, policy.nextRetryDelayMs(0))
    }

    @Test
    fun `retry 1 delay is doubled`() {
        assertEquals(4_000L, policy.nextRetryDelayMs(1))
    }

    @Test
    fun `retry 2 delay is 8000ms`() {
        assertEquals(8_000L, policy.nextRetryDelayMs(2))
    }

    @Test
    fun `negative retryCount returns base delay`() {
        assertEquals(2_000L, policy.nextRetryDelayMs(-1))
        assertEquals(2_000L, policy.nextRetryDelayMs(-10))
    }

    @Test
    fun `delay is capped at max delay`() {
        // 2000 * 2^8 = 512,000 > 300,000 cap
        assertEquals(300_000L, policy.nextRetryDelayMs(8))
        assertEquals(300_000L, policy.nextRetryDelayMs(9))
        assertEquals(300_000L, policy.nextRetryDelayMs(10))
    }

    @Test
    fun `overflow safety for very large retry counts`() {
        // Double overflow / Long overflow would happen if we don't cap early
        assertEquals(300_000L, policy.nextRetryDelayMs(1000000))
        assertEquals(300_000L, policy.nextRetryDelayMs(Int.MAX_VALUE))
    }

    @Test
    fun `PENDING status is immediately eligible`() {
        val now = Instant.now()
        val pending = PendingAckEntity(
            alertId = "ALT-001",
            acknowledgedAt = now,
            retryCount = 0,
            lastAttemptAt = null,
            status = AckSyncStatus.PENDING
        )
        assertTrue(policy.shouldRetry(pending, now))
    }

    @Test
    fun `COMPLETED status is never eligible`() {
        val now = Instant.now()
        val completed = PendingAckEntity(
            alertId = "ALT-001",
            acknowledgedAt = now,
            retryCount = 0,
            lastAttemptAt = now,
            status = AckSyncStatus.COMPLETED
        )
        assertFalse(policy.shouldRetry(completed, now))
    }

    @Test
    fun `IN_FLIGHT status is not eligible for concurrent retry`() {
        val now = Instant.now()
        val inFlight = PendingAckEntity(
            alertId = "ALT-001",
            acknowledgedAt = now,
            retryCount = 0,
            lastAttemptAt = now,
            status = AckSyncStatus.IN_FLIGHT
        )
        assertFalse(policy.shouldRetry(inFlight, now))
    }

    @Test
    fun `FAILED status is not eligible before backoff elapses`() {
        val now = Instant.now()
        val failed = PendingAckEntity(
            alertId = "ALT-001",
            acknowledgedAt = now.minusSeconds(10),
            retryCount = 1, // backoff = 4000ms
            lastAttemptAt = now.minusMillis(2000), // only 2000ms elapsed
            status = AckSyncStatus.FAILED
        )
        assertFalse(policy.shouldRetry(failed, now))
    }

    @Test
    fun `FAILED status is eligible after backoff elapses`() {
        val now = Instant.now()
        val failed = PendingAckEntity(
            alertId = "ALT-001",
            acknowledgedAt = now.minusSeconds(10),
            retryCount = 1, // backoff = 4000ms
            lastAttemptAt = now.minusMillis(4500), // 4500ms elapsed
            status = AckSyncStatus.FAILED
        )
        assertTrue(policy.shouldRetry(failed, now))
    }

    @Test
    fun `FAILED status is eligible exactly at the boundary threshold`() {
        val now = Instant.now()
        val failed = PendingAckEntity(
            alertId = "ALT-001",
            acknowledgedAt = now.minusSeconds(10),
            retryCount = 1, // backoff = 4000ms
            lastAttemptAt = now.minusMillis(4000), // Exactly 4000ms elapsed
            status = AckSyncStatus.FAILED
        )
        assertTrue(policy.shouldRetry(failed, now))
    }

    @Test
    fun `FAILED status with null lastAttemptAt is safely permitted to retry`() {
        val now = Instant.now()
        val failed = PendingAckEntity(
            alertId = "ALT-001",
            acknowledgedAt = now.minusSeconds(10),
            retryCount = 1,
            lastAttemptAt = null,
            status = AckSyncStatus.FAILED
        )
        assertTrue(policy.shouldRetry(failed, now))
    }

    @Test
    fun `exceeding max retries is not eligible`() {
        val now = Instant.now()
        val failed = PendingAckEntity(
            alertId = "ALT-001",
            acknowledgedAt = now.minusSeconds(100),
            retryCount = 10, // maxRetries = 10
            lastAttemptAt = now.minusSeconds(50),
            status = AckSyncStatus.FAILED
        )
        assertFalse(policy.shouldRetry(failed, now))
    }

    @Test
    fun `retry count greater than max retries is not eligible`() {
        val now = Instant.now()
        val failed = PendingAckEntity(
            alertId = "ALT-001",
            acknowledgedAt = now.minusSeconds(100),
            retryCount = 11, // maxRetries = 10
            lastAttemptAt = now.minusSeconds(300), // long ago
            status = AckSyncStatus.FAILED
        )
        assertFalse(policy.shouldRetry(failed, now))
    }
}
