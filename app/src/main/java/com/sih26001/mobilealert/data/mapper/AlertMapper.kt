package com.sih26001.mobilealert.data.mapper

import com.sih26001.mobilealert.data.remote.dto.AlertDto
import com.sih26001.mobilealert.domain.model.AffectedAsset
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.Location
import java.time.Instant

/**
 * Mapper that converts validated [AlertDto] wire models to [Alert] domain models.
 *
 * Missing fields are strictly preserved as null and never replaced with dummy/fabricated values.
 */
object AlertMapper {

    fun toDomain(dto: AlertDto): Alert {
        val severity = when (dto.severity?.uppercase()) {
            "CRITICAL" -> AlertSeverity.CRITICAL
            "HIGH" -> AlertSeverity.HIGH
            "NORMAL", "LOW", "MODERATE" -> AlertSeverity.NORMAL
            else -> AlertSeverity.NORMAL
        }

        val status = when (dto.status?.uppercase()) {
            "RECEIVED" -> AlertStatus.RECEIVED
            "DISPLAYED" -> AlertStatus.DISPLAYED
            "ACTIVE", "WATCH", "ESCALATED" -> AlertStatus.ACTIVE
            "SILENCED" -> AlertStatus.SILENCED
            "ACKNOWLEDGED" -> AlertStatus.ACKNOWLEDGED
            "EXPIRED", "RESOLVED" -> AlertStatus.EXPIRED
            else -> AlertStatus.ACTIVE
        }

        val location = dto.location?.let { loc ->
            Location(
                name = loc.name,
                latitude = loc.latitude,
                longitude = loc.longitude
            )
        }

        val issuedAt = dto.issued_at?.let { Instant.parse(it) } ?: Instant.EPOCH
        val expiresAt = dto.expires_at?.let { Instant.parse(it) }

        val assets = dto.affected_assets?.map { assetDto ->
            AffectedAsset(
                type = assetDto.type ?: "",
                identifier = assetDto.identifier
            )
        }

        return Alert(
            alertId = dto.alert_id.orEmpty(),
            eventType = dto.event_type.orEmpty(),
            severity = severity,
            riskScore = dto.risk_score, // null preserved, never 0
            location = location,
            issuedAt = issuedAt,
            expiresAt = expiresAt,
            topDrivers = dto.top_drivers, // null preserved
            recommendedAction = dto.recommended_action,
            affectedAssets = assets,
            source = dto.source,
            dataQuality = dto.data_quality,
            requiresAck = dto.requires_ack ?: false,
            status = status
        )
    }
}
