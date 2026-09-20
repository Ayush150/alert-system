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

/**
 * Well-known source identifier for SIH26001 Demo Protocol simulation alerts.
 */
const val DEMO_ALERT_SOURCE = "sih26001_demo"

/**
 * Returns true if this alert is explicitly a Demo Protocol simulation alert.
 * Real cloud alerts (IMD, Geological Survey, Risk Fusion, sensor networks, etc.) return false.
 */
fun Alert.isDemoAlert(): Boolean {
    return source?.equals(DEMO_ALERT_SOURCE, ignoreCase = true) == true ||
           source?.contains("demo", ignoreCase = true) == true ||
           eventType.contains("demo", ignoreCase = true) ||
           alertId.startsWith("ALT-DEMO-", ignoreCase = true) ||
           alertId.startsWith("DEMO-", ignoreCase = true)
}
