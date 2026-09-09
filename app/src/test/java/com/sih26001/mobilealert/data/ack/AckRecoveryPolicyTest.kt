package com.sih26001.mobilealert.data.ack

import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.PendingAckEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AckRecoveryPolicyTest {

    private val policy = DefaultAckRecoveryPolicy(staleThresholdMs = 300_000L) // 5 minutes

    @Test
    fun `stale IN_FLIGHT record is recoverable`() {
        val now = Instant.parse("2026-09-09T10:05:00Z")
        val ack = PendingAckEntity(
            alertId = "ALT-1",
            acknowledgedAt = Instant.parse("2026-09-09T10:00:00Z"),
            status = AckSyncStatus.IN_FLIGHT,
            lastAttemptAt = Instant.parse("2026-09-09T09:59:00Z"), // 6 minutes ago
            retryCount = 1
        )
        assertTrue(policy.isStale(ack, now))
    }

    @Test
    fun `fresh IN_FLIGHT record is not recoverable`() {
        val now = Instant.parse("2026-09-09T10:05:00Z")
        val ack = PendingAckEntity(
            alertId = "ALT-1",
            acknowledgedAt = Instant.parse("2026-09-09T10:00:00Z"),
            status = AckSyncStatus.IN_FLIGHT,
            lastAttemptAt = Instant.parse("2026-09-09T10:03:00Z"), // 2 minutes ago
            retryCount = 1
        )
        assertFalse(policy.isStale(ack, now))
    }

    @Test
    fun `IN_FLIGHT record exactly at threshold is stale`() {
        val now = Instant.parse("2026-09-09T10:05:00Z")
        val ack = PendingAckEntity(
            alertId = "ALT-1",
            acknowledgedAt = Instant.parse("2026-09-09T10:00:00Z"),
            status = AckSyncStatus.IN_FLIGHT,
            lastAttemptAt = Instant.parse("2026-09-09T10:00:00Z"), // Exactly 5 minutes ago
            retryCount = 1
        )
        assertTrue(policy.isStale(ack, now))
    }

    @Test
    fun `IN_FLIGHT record with null lastAttemptAt is conservatively considered stale`() {
        val now = Instant.now()
        val ack = PendingAckEntity(
            alertId = "ALT-1",
            acknowledgedAt = now.minusSeconds(60),
            status = AckSyncStatus.IN_FLIGHT,
            lastAttemptAt = null,
            retryCount = 1
        )
        assertTrue(policy.isStale(ack, now))
    }

    @Test
    fun `COMPLETED record is never recoverable`() {
        val now = Instant.now()
        val ack = PendingAckEntity(
            alertId = "ALT-1",
            acknowledgedAt = now.minusSeconds(600),
            status = AckSyncStatus.COMPLETED,
            lastAttemptAt = now.minusSeconds(400),
            retryCount = 1
        )
        assertFalse(policy.isStale(ack, now))
    }

    @Test
    fun `FAILED record is never recoverable`() {
        val now = Instant.now()
        val ack = PendingAckEntity(
            alertId = "ALT-1",
            acknowledgedAt = now.minusSeconds(600),
            status = AckSyncStatus.FAILED,
            lastAttemptAt = now.minusSeconds(400),
            retryCount = 1
        )
        assertFalse(policy.isStale(ack, now))
    }

    @Test
    fun `PENDING record is never recoverable`() {
        val now = Instant.now()
        val ack = PendingAckEntity(
            alertId = "ALT-1",
            acknowledgedAt = now.minusSeconds(600),
            status = AckSyncStatus.PENDING,
            lastAttemptAt = null,
            retryCount = 0
        )
        assertFalse(policy.isStale(ack, now))
    }
}
