package com.sih26001.mobilealert.domain.model

/**
 * Documented alert lifecycle states according to the SIH26001 specification:
 * - RECEIVED: Payload received on device.
 * - DISPLAYED: Rendered in user interface / notification.
 * - ACTIVE: Currently ongoing and unacknowledged/unsilenced.
 * - SILENCED: Local audio/vibration suppressed by user (local UX action, not acknowledged).
 * - ACKNOWLEDGED: Operational acknowledgement confirmed and dispatched to backend.
 * - EXPIRED: Time-to-live expired; preserved in history and no longer active.
 */
enum class AlertState {
    RECEIVED,
    DISPLAYED,
    ACTIVE,
    SILENCED,
    ACKNOWLEDGED,
    EXPIRED
}

/**
 * Alert severity levels mapped to system notification channels:
 * NORMAL, HIGH, CRITICAL.
 */
enum class AlertSeverity {
    NORMAL,
    HIGH,
    CRITICAL
}

/**
 * SIH26001 Domain Alert Model.
 *
 * NOTE:
 * - The mobile client NEVER calculates risk scores, environmental features, or ML predictions.
 * - Missing or unavailable backend values are preserved as null and never fabricated.
 * - [alertId] serves as the unique identifier for deduplication and backend operations.
 */
data class Alert(
    val alertId: String,
    val title: String,
    val description: String? = null,
    val severity: AlertSeverity,
    val state: AlertState,
    val locationName: String? = null,
    val issuedAt: Long,
    val receivedAt: Long? = null,
    val acknowledgedAt: Long? = null,
    val expiresAt: Long? = null
)
