package com.sih26001.mobilealert.domain.model

/**
 * Documented alert lifecycle states according to the SIH26001 alert contract:
 * - RECEIVED: Payload ingested on device.
 * - DISPLAYED: Rendered in user interface / notification.
 * - ACTIVE: Currently ongoing and unacknowledged/unsilenced.
 * - SILENCED: Local alarm audio/vibration suppressed by user (local UX action, not acknowledged).
 * - ACKNOWLEDGED: Operational acknowledgement confirmed by user; queued for backend sync.
 * - EXPIRED: Time-to-live expired; no longer visually active, preserved in history.
 *
 * NOTE: SILENCED is strictly distinct from ACKNOWLEDGED.
 */
enum class AlertStatus {
    RECEIVED,
    DISPLAYED,
    ACTIVE,
    SILENCED,
    ACKNOWLEDGED,
    EXPIRED
}
