package com.sih26001.mobilealert.domain.model

/**
 * Minimal domain model for client device and alert preferences.
 * Persistence (e.g. DataStore) will be added in subsequent phases.
 */
data class DeviceSettings(
    val deviceId: String? = null,
    val criticalAlertsEnabled: Boolean = true,
    val highAlertsEnabled: Boolean = true,
    val normalAlertsEnabled: Boolean = true,
    val audibleAlarmsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
