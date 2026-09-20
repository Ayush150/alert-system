package com.sih26001.mobilealert.domain.model

/**
 * Action type for a role priority card on the dashboard.
 */
enum class PriorityActionType {
    CURRENT_RISK,
    ACTIVE_ALERTS,
    SAFE_PLACES,
    UNAVAILABLE
}

/**
 * Represents a prioritized action or intelligence card tailored for a specific role.
 * Strictly adheres to the rule: NO fabricated data (no random values, fake risk scores,
 * fake incident counts, fake casualty numbers, or fake coordinates).
 */
data class RolePriority(
    val title: String,
    val iconEmoji: String,
    val actionType: PriorityActionType = PriorityActionType.UNAVAILABLE,
    val destinationRoute: String? = null,
    val isAvailable: Boolean = false,
    val placeholderStatus: String? = null
)

/**
 * Centralized dashboard configuration mapping for a [UserRole].
 * Controls role context text and priority cards without scattering role checks across UI code.
 */
data class RoleDashboardConfig(
    val role: UserRole,
    val contextText: String,
    val priorities: List<RolePriority>
) {
    companion object {
        /**
         * Resolves the centralized dashboard configuration for the given [role].
         */
        fun forRole(role: UserRole): RoleDashboardConfig {
            return when (role) {
                UserRole.CITIZEN -> RoleDashboardConfig(
                    role = UserRole.CITIZEN,
                    contextText = "Stay informed about hazards and follow official warnings.",
                    priorities = listOf(
                        RolePriority(
                            title = "Current Risk",
                            iconEmoji = "🛡️",
                            actionType = PriorityActionType.CURRENT_RISK,
                            isAvailable = true
                        ),
                        RolePriority(
                            title = "Active Alerts",
                            iconEmoji = "🔔",
                            actionType = PriorityActionType.ACTIVE_ALERTS,
                            destinationRoute = "alerts",
                            isAvailable = true
                        ),
                        RolePriority(
                            title = "Safe Places",
                            iconEmoji = "📍",
                            actionType = PriorityActionType.SAFE_PLACES,
                            destinationRoute = "safe_place",
                            isAvailable = true
                        ),
                        RolePriority(
                            title = "Report Hazard",
                            iconEmoji = "⚠️",
                            actionType = PriorityActionType.UNAVAILABLE,
                            isAvailable = false,
                            placeholderStatus = "Data unavailable"
                        )
                    )
                )

                UserRole.POLICE -> RoleDashboardConfig(
                    role = UserRole.POLICE,
                    contextText = "Monitor affected areas and coordinate local emergency response.",
                    priorities = listOf(
                        RolePriority(
                            title = "Active Alerts",
                            iconEmoji = "🚨",
                            actionType = PriorityActionType.ACTIVE_ALERTS,
                            destinationRoute = "alerts",
                            isAvailable = true
                        ),
                        RolePriority(
                            title = "Affected Areas",
                            iconEmoji = "🗺️",
                            actionType = PriorityActionType.UNAVAILABLE,
                            isAvailable = false,
                            placeholderStatus = "Data unavailable"
                        ),
                        RolePriority(
                            title = "Road / Infrastructure Exposure",
                            iconEmoji = "🚧",
                            actionType = PriorityActionType.UNAVAILABLE,
                            isAvailable = false,
                            placeholderStatus = "Data unavailable"
                        ),
                        RolePriority(
                            title = "Response Coordination",
                            iconEmoji = "📻",
                            actionType = PriorityActionType.UNAVAILABLE,
                            isAvailable = false,
                            placeholderStatus = "No active incidents"
                        )
                    )
                )

                UserRole.RESCUE_TEAM -> RoleDashboardConfig(
                    role = UserRole.RESCUE_TEAM,
                    contextText = "Prioritise rescue operations using current risk intelligence.",
                    priorities = listOf(
                        RolePriority(
                            title = "Critical / High Risk Zones",
                            iconEmoji = "⚠️",
                            actionType = PriorityActionType.ACTIVE_ALERTS,
                            destinationRoute = "alerts",
                            isAvailable = true
                        ),
                        RolePriority(
                            title = "Active Alerts",
                            iconEmoji = "🚨",
                            actionType = PriorityActionType.ACTIVE_ALERTS,
                            destinationRoute = "alerts",
                            isAvailable = true
                        ),
                        RolePriority(
                            title = "Rescue Priorities",
                            iconEmoji = "🎯",
                            actionType = PriorityActionType.UNAVAILABLE,
                            isAvailable = false,
                            placeholderStatus = "No active incidents"
                        ),
                        RolePriority(
                            title = "Affected Infrastructure",
                            iconEmoji = "🏗️",
                            actionType = PriorityActionType.UNAVAILABLE,
                            isAvailable = false,
                            placeholderStatus = "Data unavailable"
                        )
                    )
                )

                UserRole.DISTRICT_ADMINISTRATION -> RoleDashboardConfig(
                    role = UserRole.DISTRICT_ADMINISTRATION,
                    contextText = "Monitor regional conditions and coordinate emergency response.",
                    priorities = listOf(
                        RolePriority(
                            title = "Regional Risk Overview",
                            iconEmoji = "📊",
                            actionType = PriorityActionType.CURRENT_RISK,
                            isAvailable = true
                        ),
                        RolePriority(
                            title = "Active Alerts",
                            iconEmoji = "🔔",
                            actionType = PriorityActionType.ACTIVE_ALERTS,
                            destinationRoute = "alerts",
                            isAvailable = true
                        ),
                        RolePriority(
                            title = "Infrastructure Exposure",
                            iconEmoji = "🏢",
                            actionType = PriorityActionType.UNAVAILABLE,
                            isAvailable = false,
                            placeholderStatus = "Data unavailable"
                        ),
                        RolePriority(
                            title = "Response Status",
                            iconEmoji = "📋",
                            actionType = PriorityActionType.UNAVAILABLE,
                            isAvailable = false,
                            placeholderStatus = "No active incidents"
                        )
                    )
                )

                UserRole.EMERGENCY_SERVICES -> RoleDashboardConfig(
                    role = UserRole.EMERGENCY_SERVICES,
                    contextText = "Coordinate emergency operations using live risk intelligence.",
                    priorities = listOf(
                        RolePriority(
                            title = "Active Alerts",
                            iconEmoji = "🔔",
                            actionType = PriorityActionType.ACTIVE_ALERTS,
                            destinationRoute = "alerts",
                            isAvailable = true
                        ),
                        RolePriority(
                            title = "High/Critical Zones",
                            iconEmoji = "🚨",
                            actionType = PriorityActionType.ACTIVE_ALERTS,
                            destinationRoute = "alerts",
                            isAvailable = true
                        ),
                        RolePriority(
                            title = "Infrastructure Exposure",
                            iconEmoji = "⚡",
                            actionType = PriorityActionType.UNAVAILABLE,
                            isAvailable = false,
                            placeholderStatus = "Data unavailable"
                        ),
                        RolePriority(
                            title = "Response Status",
                            iconEmoji = "📡",
                            actionType = PriorityActionType.UNAVAILABLE,
                            isAvailable = false,
                            placeholderStatus = "No active incidents"
                        )
                    )
                )
            }
        }
    }
}
