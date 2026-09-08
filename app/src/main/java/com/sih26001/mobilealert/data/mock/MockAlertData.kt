package com.sih26001.mobilealert.data.mock

import com.sih26001.mobilealert.domain.model.AffectedAsset
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.Location
import java.time.Instant

object MockAlertData {
    
    val normalAlert = Alert(
        alertId = "mock-alert-normal-001",
        eventType = "Heavy Rain Warning",
        severity = AlertSeverity.NORMAL,
        riskScore = 0.3,
        location = Location(latitude = 28.6139, longitude = 77.2090),
        issuedAt = Instant.now(),
        expiresAt = Instant.now().plusSeconds(3600),
        topDrivers = listOf("Rainfall intensity", "Soil moisture"),
        recommendedAction = "Stay updated with local weather reports.",
        affectedAssets = emptyList(),
        source = "IMD",
        dataQuality = "HIGH",
        requiresAck = false,
        status = AlertStatus.ACTIVE,
        receivedAt = Instant.now(),
        acknowledgedAt = null
    )

    val highAlert = Alert(
        alertId = "mock-alert-high-002",
        eventType = "Landslide Warning",
        severity = AlertSeverity.HIGH,
        riskScore = 0.75,
        location = Location(latitude = 27.0466, longitude = 88.2625),
        issuedAt = Instant.now(),
        expiresAt = Instant.now().plusSeconds(7200),
        topDrivers = listOf("Continuous rainfall", "Steep slope"),
        recommendedAction = "Avoid travel in hilly areas. Be prepared to evacuate if advised.",
        affectedAssets = listOf(
            AffectedAsset(type = AffectedAsset.TYPE_ROAD, identifier = "road-1")
        ),
        source = "Geological Survey",
        dataQuality = "HIGH",
        requiresAck = true,
        status = AlertStatus.ACTIVE,
        receivedAt = Instant.now(),
        acknowledgedAt = null
    )

    val criticalAlert = Alert(
        alertId = "mock-alert-critical-003",
        eventType = "Imminent Landslide",
        severity = AlertSeverity.CRITICAL,
        riskScore = 0.95,
        location = Location(latitude = 31.1048, longitude = 77.1666),
        issuedAt = Instant.now(),
        expiresAt = Instant.now().plusSeconds(1800),
        topDrivers = listOf("Slope failure detected", "Ground vibration"),
        recommendedAction = "EVACUATE IMMEDIATELY to higher ground or designated shelters.",
        affectedAssets = listOf(
            AffectedAsset(type = AffectedAsset.TYPE_ROAD, identifier = "Highway"),
            AffectedAsset(type = AffectedAsset.TYPE_SETTLEMENT, identifier = "village-a")
        ),
        source = "Risk Engine",
        dataQuality = "HIGH",
        requiresAck = true,
        status = AlertStatus.ACTIVE,
        receivedAt = Instant.now(),
        acknowledgedAt = null
    )
}
