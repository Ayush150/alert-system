package com.sih26001.mobilealert.domain.model

/**
 * Structured safe place destination for emergency evacuation guidance.
 * Adheres strictly to the rule: NO random coordinates or fabricated dynamic locations.
 */
data class SafePlaceDestination(
    val name: String,
    val distance: String,
    val estimatedTime: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDemo: Boolean = false,
    val statusDescription: String? = null
)
