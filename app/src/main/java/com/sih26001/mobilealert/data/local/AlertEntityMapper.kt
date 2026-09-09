package com.sih26001.mobilealert.data.local

import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.Location

fun Alert.toEntity(): AlertEntity {
    return AlertEntity(
        alertId = alertId,
        eventType = eventType,
        severity = severity,
        riskScore = riskScore,
        locationName = location?.name,
        latitude = location?.latitude,
        longitude = location?.longitude,
        issuedAt = issuedAt,
        expiresAt = expiresAt,
        topDrivers = topDrivers,
        recommendedAction = recommendedAction,
        affectedAssets = affectedAssets,
        source = source,
        dataQuality = dataQuality,
        requiresAck = requiresAck,
        status = status,
        receivedAt = receivedAt,
        acknowledgedAt = acknowledgedAt
    )
}

fun AlertEntity.toDomain(): Alert {
    val locationObj = if (locationName != null && latitude != null && longitude != null) {
        Location(
            name = locationName,
            latitude = latitude,
            longitude = longitude
        )
    } else {
        null
    }

    return Alert(
        alertId = alertId,
        eventType = eventType,
        severity = severity,
        riskScore = riskScore,
        location = locationObj,
        issuedAt = issuedAt,
        expiresAt = expiresAt,
        topDrivers = topDrivers,
        recommendedAction = recommendedAction,
        affectedAssets = affectedAssets,
        source = source,
        dataQuality = dataQuality,
        requiresAck = requiresAck,
        status = status,
        receivedAt = receivedAt,
        acknowledgedAt = acknowledgedAt
    )
}
