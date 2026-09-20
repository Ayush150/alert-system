package com.sih26001.mobilealert.data.fcm

import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.repository.AlertRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Phase 4A — Production FCM Foundation Tests.
 *
 * Verifies:
 * - Test A: Token callback does not crash.
 * - Test B: Token privacy masking ensures full sensitive token is never emitted.
 * - Test C: Valid data-only FCM payload contract {"alert_id": "..."} continues through pipeline.
 * - Test D: Missing alert_id rejects payload without initiating authoritative fetch.
 * - Test E: Blank/whitespace alert_id rejects payload without persisting alert.
 * - Payload contract: FCM contains only trigger alert_id; authoritative fields in FCM are ignored.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Phase4AFcmProductionFoundationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAlertRepository: AlertRepository = mock()
    private val mockNotificationManager: AlertNotificationManager = mock()
    private val mockAlarmController: AlarmController = mock()

    private lateinit var processor: FcmMessageProcessor
    private lateinit var triggerHandler: FcmAlertTriggerHandlerImpl

    @Before
    fun setUp() {
        processor = FcmMessageProcessor(maxCacheSize = 100)
        triggerHandler = FcmAlertTriggerHandlerImpl(
            alertRepository = mockAlertRepository,
            notificationManager = mockNotificationManager,
            alarmController = mockAlarmController,
            coroutineScope = testScope
        )
    }

    // =========================================================================
    // Test A — Token Callback Does Not Crash
    // =========================================================================
    @Test
    fun `Test A - token callback does not crash with arbitrary or empty strings`() {
        // Test with arbitrary realistic tokens
        val service = SihFirebaseMessagingService()
        service.onNewToken("fcm_token_sample_abc1234567890")
        service.onNewToken("")
        service.onNewToken("short")
    }

    // =========================================================================
    // Test B — Token Privacy: Full Token Is Never Emitted
    // =========================================================================
    @Test
    fun `Test B - token privacy masking preserves confidentiality`() {
        val sampleToken = "fcm_token_super_secret_production_client_key_9876543210_abcdef"
        val masked = SihFirebaseMessagingService.maskToken(sampleToken)

        // Must never equal the full raw token
        assertNotEquals(sampleToken, masked)

        // Must contain preview prefix and suffix
        assertEquals("fcm_...cdef", masked)

        // Must NOT contain sensitive interior characters
        assertFalse(masked.contains("secret"))
        assertFalse(masked.contains("production"))
        assertFalse(masked.contains("9876543210"))

        // Short or empty tokens must fall back safely
        assertEquals("***", SihFirebaseMessagingService.maskToken(null))
        assertEquals("***", SihFirebaseMessagingService.maskToken(""))
        assertEquals("***", SihFirebaseMessagingService.maskToken("12345678"))
    }

    // =========================================================================
    // Test C — Valid Data-Only Payload Triggers Authoritative Pipeline
    // =========================================================================
    @Test
    fun `Test C - valid data-only payload triggers authoritative REST fetch`() = testScope.runTest {
        val pilotAlertId = "ALT-TAW-1789811514-4E51"
        val payload = mapOf("alert_id" to pilotAlertId)

        val authoritativeAlert = Alert(
            alertId = pilotAlertId,
            eventType = "LANDSLIDE_WARNING",
            severity = AlertSeverity.HIGH,
            riskScore = 68.0,
            location = null,
            issuedAt = Instant.now(),
            expiresAt = null,
            topDrivers = listOf("Rainfall", "Kinematics"),
            recommendedAction = "HEIGHTENED WARNING: Restrict arterial traffic.",
            affectedAssets = null,
            source = "risk_fusion",
            dataQuality = "FULL",
            requiresAck = true,
            status = AlertStatus.ACTIVE
        )

        whenever(mockAlertRepository.refreshAlert(pilotAlertId)).thenReturn(Result.success(authoritativeAlert))

        val result = processor.processData(payload)
        assertTrue("Expected ValidAlert for valid alert_id", result is FcmProcessingResult.ValidAlert)

        val validAlertId = (result as FcmProcessingResult.ValidAlert).alertId
        assertEquals(pilotAlertId, validAlertId)

        triggerHandler.handleAlertTrigger(validAlertId)
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert(pilotAlertId)
        verify(mockNotificationManager).showAlertNotification(authoritativeAlert)
        verify(mockAlarmController, never()).startAlarm(any())
    }

    // =========================================================================
    // Test D — Missing alert_id Never Initiates Authoritative Fetch
    // =========================================================================
    @Test
    fun `Test D - missing alert_id does not trigger authoritative fetch`() = testScope.runTest {
        val emptyPayload = emptyMap<String, String>()
        val resultEmpty = processor.processData(emptyPayload)
        assertTrue(resultEmpty is FcmProcessingResult.EmptyPayload)

        val missingKeyPayload = mapOf("other_data" to "some_value")
        val resultMissing = processor.processData(missingKeyPayload)
        assertTrue(resultMissing is FcmProcessingResult.MissingAlertId)

        advanceUntilIdle()
        verify(mockAlertRepository, never()).refreshAlert(any())
    }

    // =========================================================================
    // Test E — Malformed or Blank alert_id Never Initiates Fetch Or Persists
    // =========================================================================
    @Test
    fun `Test E - blank or whitespace alert_id is rejected safely`() = testScope.runTest {
        val blankResult = processor.processData(mapOf("alert_id" to ""))
        assertTrue(blankResult is FcmProcessingResult.MissingAlertId)

        val whitespaceResult = processor.processData(mapOf("alert_id" to "   "))
        assertTrue(whitespaceResult is FcmProcessingResult.MissingAlertId)

        advanceUntilIdle()
        verify(mockAlertRepository, never()).refreshAlert(any())
    }

    // =========================================================================
    // Payload Contract — Authoritative Fields in FCM Are Ignored
    // =========================================================================
    @Test
    fun `Payload Contract - authoritative fields in FCM are ignored and alert_id is strictly trigger`() {
        val maliciousOrNonContractPayload = mapOf(
            "alert_id" to "ALT-TAW-1789811514-4E51",
            "severity" to "CRITICAL",
            "risk_score" to "99.9",
            "location" to "Fake Location",
            "status" to "RESOLVED"
        )

        val result = processor.processData(maliciousOrNonContractPayload)
        assertTrue(result is FcmProcessingResult.ValidAlert)

        // Processor only returns the alertId; it does not parse or trust the extra fields
        val alertId = (result as FcmProcessingResult.ValidAlert).alertId
        assertEquals("ALT-TAW-1789811514-4E51", alertId)
    }
}
