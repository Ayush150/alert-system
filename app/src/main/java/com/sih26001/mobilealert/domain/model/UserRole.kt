package com.sih26001.mobilealert.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sih26001.mobilealert.R

/**
 * Demo-only user roles for GeoAgent.
 * These are not tied to any authentication or backend identity system.
 */
enum class UserRole(
    val displayName: String,
    val description: String,
    val emoji: String
) {
    CITIZEN(
        displayName = "Citizen",
        description = "Receive alerts and evacuation guidance for your area",
        emoji = "👤"
    ),
    POLICE(
        displayName = "Police",
        description = "Coordinate law enforcement response and area control",
        emoji = "🚔"
    ),
    RESCUE_TEAM(
        displayName = "Rescue Team",
        description = "Deploy search-and-rescue operations in affected zones",
        emoji = "🚑"
    ),
    DISTRICT_ADMINISTRATION(
        displayName = "District Administration",
        description = "Oversee district-wide disaster response and resource allocation",
        emoji = "🏛️"
    ),
    EMERGENCY_SERVICES(
        displayName = "Emergency Services",
        description = "Manage emergency infrastructure and medical response",
        emoji = "🆘"
    );

    @Composable
    fun getLocalizedName(): String = when (this) {
        CITIZEN -> stringResource(R.string.role_citizen_name)
        POLICE -> stringResource(R.string.role_police_name)
        RESCUE_TEAM -> stringResource(R.string.role_rescue_name)
        DISTRICT_ADMINISTRATION -> stringResource(R.string.role_admin_name)
        EMERGENCY_SERVICES -> stringResource(R.string.role_emergency_name)
    }

    @Composable
    fun getLocalizedDesc(): String = when (this) {
        CITIZEN -> stringResource(R.string.role_citizen_desc)
        POLICE -> stringResource(R.string.role_police_desc)
        RESCUE_TEAM -> stringResource(R.string.role_rescue_desc)
        DISTRICT_ADMINISTRATION -> stringResource(R.string.role_admin_desc)
        EMERGENCY_SERVICES -> stringResource(R.string.role_emergency_desc)
    }

    companion object {
        /**
         * Safely converts a stored string key back to a [UserRole], or null if invalid.
         */
        fun fromKey(key: String?): UserRole? {
            if (key.isNullOrBlank()) return null
            return try {
                valueOf(key)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
