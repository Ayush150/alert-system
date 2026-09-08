package com.sih26001.mobilealert.domain.model

/**
 * Represents infrastructure or assets impacted by a hazard event.
 * Currently demonstrates "road" and "settlement" assets per the SIH26001 contract.
 * Designed to be easily extensible without coupling to UI components.
 */
data class AffectedAsset(
    val type: String,
    val identifier: String? = null
) {
    companion object {
        const val TYPE_ROAD = "road"
        const val TYPE_SETTLEMENT = "settlement"
    }
}
