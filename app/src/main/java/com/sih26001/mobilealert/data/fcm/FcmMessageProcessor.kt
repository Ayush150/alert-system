package com.sih26001.mobilealert.data.fcm

import java.util.Collections
import java.util.LinkedHashMap

sealed interface FcmProcessingResult {
    data class ValidAlert(val alertId: String) : FcmProcessingResult
    object MissingAlertId : FcmProcessingResult
    data class DuplicateAlert(val alertId: String) : FcmProcessingResult
    object EmptyPayload : FcmProcessingResult
}

/**
 * Pure testable processor for FCM message data payloads.
 * Handles payload extraction, validation, and in-memory deduplication.
 * Treats alert_id strictly as an identifier/trigger rather than authoritative alert data.
 */
class FcmMessageProcessor(
    private val maxCacheSize: Int = 100
) {
    // Thread-safe LRU cache to detect duplicate alert_ids within a session
    private val seenAlertIds = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(maxCacheSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > maxCacheSize
            }
        }
    )

    fun processData(data: Map<String, String>?): FcmProcessingResult {
        if (data.isNullOrEmpty()) {
            return FcmProcessingResult.EmptyPayload
        }

        val rawAlertId = data["alert_id"]?.trim()
        if (rawAlertId.isNullOrEmpty()) {
            return FcmProcessingResult.MissingAlertId
        }

        synchronized(seenAlertIds) {
            if (seenAlertIds.containsKey(rawAlertId)) {
                return FcmProcessingResult.DuplicateAlert(rawAlertId)
            }
            seenAlertIds[rawAlertId] = System.currentTimeMillis()
        }

        return FcmProcessingResult.ValidAlert(rawAlertId)
    }

    fun clearCacheForTesting() {
        seenAlertIds.clear()
    }
}
