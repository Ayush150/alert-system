package com.sih26001.mobilealert.data.remote.dto

/**
 * Data Transfer Object representing the raw alert wire format from the SIH26001 backend.
 *
 * All fields are nullable at the boundary to allow safe validation of incomplete
 * or malformed payloads before domain conversion.
 */
data class AlertDto(
    val alert_id: String? = null,
    val event_type: String? = null,
    val severity: String? = null,
    val risk_score: Double? = null,
    val location: LocationDto? = null,
    val issued_at: String? = null,
    val expires_at: String? = null,
    val top_drivers: List<String>? = null,
    val recommended_action: String? = null,
    val affected_assets: List<AffectedAssetDto>? = null,
    val source: String? = null,
    val data_quality: String? = null,
    val requires_ack: Boolean? = null,
    val status: String? = null
)

data class LocationDto(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class AffectedAssetDto(
    val type: String? = null,
    val identifier: String? = null
)
