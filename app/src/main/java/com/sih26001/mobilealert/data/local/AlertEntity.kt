package com.sih26001.mobilealert.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sih26001.mobilealert.domain.model.AffectedAsset
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import java.time.Instant

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val alertId: String,
    val eventType: String,
    val severity: AlertSeverity,
    val riskScore: Double?,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val issuedAt: Instant,
    val expiresAt: Instant?,
    val topDrivers: List<String>?,
    val recommendedAction: String?,
    val affectedAssets: List<AffectedAsset>?,
    val source: String?,
    val dataQuality: String?,
    val requiresAck: Boolean,
    val status: AlertStatus,
    val receivedAt: Instant?,
    val acknowledgedAt: Instant?
)
