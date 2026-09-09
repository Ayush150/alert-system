package com.sih26001.mobilealert.data.fcm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FcmMessageProcessorTest {

    private lateinit var processor: FcmMessageProcessor

    @Before
    fun setUp() {
        processor = FcmMessageProcessor(maxCacheSize = 10)
        processor.clearCacheForTesting()
    }

    @Test
    fun `processData with valid alert_id returns ValidAlert`() {
        val payload = mapOf("alert_id" to "ALT-2026-000123", "extra" to "value")
        val result = processor.processData(payload)

        assertTrue(result is FcmProcessingResult.ValidAlert)
        assertEquals("ALT-2026-000123", (result as FcmProcessingResult.ValidAlert).alertId)
    }

    @Test
    fun `processData with whitespace in alert_id trims properly`() {
        val payload = mapOf("alert_id" to "  ALT-2026-000124  ")
        val result = processor.processData(payload)

        assertTrue(result is FcmProcessingResult.ValidAlert)
        assertEquals("ALT-2026-000124", (result as FcmProcessingResult.ValidAlert).alertId)
    }

    @Test
    fun `processData with missing alert_id returns MissingAlertId`() {
        val payload = mapOf("other_field" to "some_value")
        val result = processor.processData(payload)

        assertTrue(result is FcmProcessingResult.MissingAlertId)
    }

    @Test
    fun `processData with empty string alert_id returns MissingAlertId`() {
        val payload = mapOf("alert_id" to "")
        val result = processor.processData(payload)

        assertTrue(result is FcmProcessingResult.MissingAlertId)
    }

    @Test
    fun `processData with blank string alert_id returns MissingAlertId`() {
        val payload = mapOf("alert_id" to "   ")
        val result = processor.processData(payload)

        assertTrue(result is FcmProcessingResult.MissingAlertId)
    }

    @Test
    fun `processData with null map returns EmptyPayload`() {
        val result = processor.processData(null)

        assertTrue(result is FcmProcessingResult.EmptyPayload)
    }

    @Test
    fun `processData with empty map returns EmptyPayload`() {
        val result = processor.processData(emptyMap())

        assertTrue(result is FcmProcessingResult.EmptyPayload)
    }

    @Test
    fun `processData with duplicate alert_id returns DuplicateAlert on second call`() {
        val payload = mapOf("alert_id" to "ALT-2026-000999")

        val firstResult = processor.processData(payload)
        assertTrue(firstResult is FcmProcessingResult.ValidAlert)

        val secondResult = processor.processData(payload)
        assertTrue(secondResult is FcmProcessingResult.DuplicateAlert)
        assertEquals("ALT-2026-000999", (secondResult as FcmProcessingResult.DuplicateAlert).alertId)
    }

    @Test
    fun `processData after cache clear allows processing previously seen alert_id`() {
        val payload = mapOf("alert_id" to "ALT-2026-000999")

        val firstResult = processor.processData(payload)
        assertTrue(firstResult is FcmProcessingResult.ValidAlert)

        processor.clearCacheForTesting()

        val afterClearResult = processor.processData(payload)
        assertTrue(afterClearResult is FcmProcessingResult.ValidAlert)
    }
}
