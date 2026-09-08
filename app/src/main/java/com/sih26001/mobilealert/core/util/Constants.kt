package com.sih26001.mobilealert.core.util

/**
 * Global constant definitions for navigation routes and notification channels.
 */
object Constants {
    // Navigation Routes
    const val ROUTE_HOME = "home"
    const val ROUTE_ALERTS = "alerts"
    const val ROUTE_HISTORY = "history"

    // Notification Channel IDs (Phase 1 contract specification per Rule 9)
    const val CHANNEL_ID_NORMAL = "channel_alert_normal"
    const val CHANNEL_ID_HIGH = "channel_alert_high"
    const val CHANNEL_ID_CRITICAL = "channel_alert_critical"
}
