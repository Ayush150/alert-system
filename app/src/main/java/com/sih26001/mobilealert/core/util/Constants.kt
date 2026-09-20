package com.sih26001.mobilealert.core.util

/**
 * Global constant definitions for navigation routes and notification channels.
 */
object Constants {
    // Navigation Routes
    const val ROUTE_HOME = "home"
    const val ROUTE_ALERTS = "alerts"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_SAFE_PLACE = "safe_place"
    const val ROUTE_ROUTE = "route"
    const val ROUTE_ROLE_SELECTION = "role_selection"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_EMERGENCY = "emergency_alert"

    // Notification Channel IDs (Phase 1 contract specification per Rule 9)
    const val CHANNEL_ID_NORMAL = "channel_alert_normal"
    const val CHANNEL_ID_HIGH = "channel_alert_high"
    const val CHANNEL_ID_CRITICAL = "channel_alert_critical"

    // Firebase Cloud Messaging Topic (Authoritative alert broadcast channel)
    const val FCM_TOPIC_ALERTS = "sih26001_alerts"
}
