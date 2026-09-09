package com.sih26001.mobilealert.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class AckSyncStatus {
    PENDING,
    IN_FLIGHT,
    FAILED,
    COMPLETED
}

/**
 * Room entity representing an operational acknowledgement queued for backend synchronization.
 * Survives process death to ensure offline reliability.
 */
@Entity(tableName = "pending_acks")
data class PendingAckEntity(
    @PrimaryKey val alertId: String,
    val acknowledgedAt: Instant,
    val retryCount: Int = 0,
    val lastAttemptAt: Instant? = null,
    val status: AckSyncStatus
)
