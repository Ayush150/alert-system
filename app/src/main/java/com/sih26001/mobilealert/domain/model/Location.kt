package com.sih26001.mobilealert.domain.model

/**
 * Geographic location information associated with an alert.
 * Missing numerical or textual coordinates must remain null and never be fabricated.
 */
data class Location(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
