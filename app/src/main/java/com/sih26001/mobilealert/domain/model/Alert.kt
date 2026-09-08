package com.sih26001.mobilealert.domain.model

import java.time.Instant

/**
 * Stable SIH26001 Alert Domain Model.
 *
 * CONTRACT RULES:
 * 1. The mobile client NEVER calculates risk scores, environmental features, or ML predictions.
 *    Risk calculation is performed exclusively by the upstream Risk Engine.
 * 2. Missing data is preserved as null and never replaced with 0 or default fallbacks.
 * 3. [alertId] serves as the unique identifier for deduplication and backend lifecycle events.
 * 4. [status] tracks the lifecycle state, strictly distinguishing SILENCED from ACKNOWLEDGED.
 */
data class Alert(
    val alertId: String,
    val eventType: String,
    val severity: AlertSeverity,
    val riskScore: Double? = null,
    val location: Location? = null,
    val issuedAt: Instant,
    val expiresAt: Instant? = null,
    val topDrivers: List<String>? = null,
    val recommendedAction: String? = null,
    val affectedAssets: List<AffectedAsset>? = null,
    val source: String? = null,
    val dataQuality: String? = null,
    val requiresAck: Boolean = false,
    val status: AlertStatus = AlertStatus.ACTIVE,
    val receivedAt: Instant? = null,
    val acknowledgedAt: Instant? = null
)
