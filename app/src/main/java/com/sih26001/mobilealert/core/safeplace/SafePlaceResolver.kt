package com.sih26001.mobilealert.core.safeplace

import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.SafePlaceDestination

/**
 * Authoritative destination resolver for emergency evacuation.
 *
 * Rules:
 * 1. Checks backend authoritative assets first (e.g. shelter, safe_zone, relief_center).
 * 2. If absent, uses a clearly labelled DEMO safe-place configuration with fixed documented locations.
 * 3. Never generates random coordinates or random distances.
 */
object SafePlaceResolver {

    // Documented, fixed demo destinations with official coordinates
    private val DEMO_DESTINATIONS = mapOf(
        "shillong" to SafePlaceDestination(
            name = "Shillong Community Relief Centre",
            distance = "1.8 km",
            estimatedTime = "Approx. 7 min",
            latitude = 25.5788,
            longitude = 91.8933,
            isDemo = true,
            statusDescription = "Designated Demo Evacuation Facility"
        ),
        "tawang" to SafePlaceDestination(
            name = "Tawang Community Relief Centre",
            distance = "2.3 km",
            estimatedTime = "Approx. 10 min",
            latitude = 27.5860,
            longitude = 91.8650,
            isDemo = true,
            statusDescription = "Designated Demo Evacuation Facility"
        )
    )

    fun resolveDestination(alert: Alert?): SafePlaceDestination {
        if (alert == null) {
            return SafePlaceDestination(
                name = "Designated Emergency Relief Centre",
                distance = "1.8 km",
                estimatedTime = "Approx. 7 min",
                latitude = 25.5788,
                longitude = 91.8933,
                isDemo = true,
                statusDescription = "Designated Demo Evacuation Facility"
            )
        }

        // 1. Check for real authoritative backend assets if provided
        val safeAsset = alert.affectedAssets?.firstOrNull {
            it.type.equals("shelter", ignoreCase = true) ||
            it.type.equals("safe_zone", ignoreCase = true) ||
            it.type.equals("relief_center", ignoreCase = true)
        }
        if (safeAsset != null && !safeAsset.identifier.isNullOrBlank()) {
            return SafePlaceDestination(
                name = safeAsset.identifier,
                distance = "Nearby",
                estimatedTime = "Immediate",
                latitude = alert.location?.latitude,
                longitude = alert.location?.longitude,
                isDemo = false,
                statusDescription = "Verified Official Emergency Shelter"
            )
        }

        // 2. Fixed documented demo destination matching location name
        val locKey = alert.location?.name?.lowercase()?.trim() ?: ""
        val matched = DEMO_DESTINATIONS.entries.firstOrNull { locKey.contains(it.key) }?.value
        if (matched != null) {
            return matched
        }

        val areaName = alert.location?.name?.takeIf { it.isNotBlank() } ?: "District"
        return SafePlaceDestination(
            name = "$areaName Community Relief Centre",
            distance = "1.8 km",
            estimatedTime = "Approx. 7 min",
            latitude = 25.5788,
            longitude = 91.8933,
            isDemo = true,
            statusDescription = "Designated Demo Evacuation Facility"
        )
    }
}
