package com.sih26001.mobilealert.data.remote.dto

/**
 * DTO for operational acknowledgment request sent to POST /api/v1/alerts/{alert_id}/ack.
 */
data class AckRequestDto(
    val alert_id: String? = null,
    val acknowledged_by: String? = "Android User",
    val notes: String? = "Operational acknowledgement confirmed on Android mobile device"
)

/**
 * DTO for operational acknowledgment response returned by POST /api/v1/alerts/{alert_id}/ack.
 */
data class AckResponseDto(
    val alert_id: String? = null,
    val status: String? = null,
    val timestamp: String? = null,
    val mqtt_published: Boolean? = null
)

/**
 * DTO for silence response returned by POST /api/v1/alerts/silence.
 */
data class SilenceResponseDto(
    val status: String? = null,
    val is_muted: Boolean? = null,
    val alarm_state: String? = null,
    val active_alert_id: String? = null
)

/**
 * DTO for ESP32 and Virtual Alarm status returned by GET /api/v1/alerts/status.
 */
data class AlarmStatusDto(
    val virtual_alarm_active: Boolean? = null,
    val physical_esp32_connected: Boolean? = null,
    val physical_esp32_last_seen: String? = null,
    val current_alarm_state: String? = null,
    val is_muted: Boolean? = null,
    val active_alert: AlertDto? = null,
    val recent_mqtt_topic: String? = null
)

/**
 * DTO for system health and gateway status returned by GET /api/v1/system/status.
 */
data class SystemStatusDto(
    val api: Map<String, Any?>? = null,
    val database: Map<String, Any?>? = null,
    val sensor_pipeline: Map<String, Any?>? = null,
    val risk_engine: Map<String, Any?>? = null,
    val timestamp: String? = null,
    val environment: String? = null
)
