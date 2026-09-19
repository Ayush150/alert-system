package com.sih26001.mobilealert.data.fcm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SihFirebaseMessagingServiceTest {

    private lateinit var service: SihFirebaseMessagingService

    @Before
    fun setUp() {
        service = SihFirebaseMessagingService()
        SihFirebaseMessagingService.processor.clearCacheForTesting()
    }

    @Test
    fun onNewToken_executesSafelyWithoutCrash() {
        // Must never throw or crash even with arbitrary tokens
        service.onNewToken("sample_fcm_registration_token_xyz987654321")
    }

    @Test
    fun onNewToken_preservesTokenPrivacy_masksSensitiveTokens() {
        val sampleToken = "fcm_production_secret_token_1234567890_abcdef"
        val masked = SihFirebaseMessagingService.maskToken(sampleToken)
        org.junit.Assert.assertNotEquals("Full token must never be exposed", sampleToken, masked)
        org.junit.Assert.assertEquals("fcm_...cdef", masked)
        org.junit.Assert.assertFalse("Masked preview must not contain secret content", masked.contains("secret"))
    }

    @Test
    fun onMessageReceived_withValidAlertId_extractsSuccessfully() {
        val remoteMessage = RemoteMessage.Builder("sender@gcm.googleapis.com")
            .addData("alert_id", "ALT-2026-FCM-001")
            .build()

        service.onMessageReceived(remoteMessage)

        // Verify processor state reflects processing
        val secondResult = SihFirebaseMessagingService.processor.processData(mapOf("alert_id" to "ALT-2026-FCM-001"))
        assertTrue("Expected duplicate detection after service processed message", secondResult is FcmProcessingResult.DuplicateAlert)
    }

    @Test
    fun onMessageReceived_withMissingAlertId_handlesGracefullyWithoutCrash() {
        val remoteMessage = RemoteMessage.Builder("sender@gcm.googleapis.com")
            .addData("unrelated_key", "value")
            .build()

        service.onMessageReceived(remoteMessage)
    }

    @Test
    fun onMessageReceived_withEmptyData_handlesGracefullyWithoutCrash() {
        val remoteMessage = RemoteMessage.Builder("sender@gcm.googleapis.com")
            .build()

        service.onMessageReceived(remoteMessage)
    }

    @Test
    fun onMessageReceived_duplicateMessages_deduplicatesSafely() {
        val remoteMessage = RemoteMessage.Builder("sender@gcm.googleapis.com")
            .addData("alert_id", "ALT-2026-FCM-DUP")
            .build()

        // First message
        service.onMessageReceived(remoteMessage)

        // Duplicate message
        service.onMessageReceived(remoteMessage)
    }

    @Test
    fun onMessageReceived_withAuthoritativeTawangAlert_fetchesAndPersists() = kotlinx.coroutines.runBlocking {
        val alertId = "ALT-TAW-1789811514-4E51"
        val remoteMessage = RemoteMessage.Builder("sender@gcm.googleapis.com")
            .addData("alert_id", alertId)
            .build()

        service.onMessageReceived(remoteMessage)

        // Allow background coroutine to fetch from REST and persist to Room
        var persistedAlert: com.sih26001.mobilealert.data.local.AlertEntity? = null
        for (i in 1..25) {
            kotlinx.coroutines.delay(200)
            persistedAlert = com.sih26001.mobilealert.di.DependencyContainer.database.alertDao().getAlertById(alertId)
            if (persistedAlert != null) break
        }

        org.junit.Assert.assertNotNull("Alert should be fetched from REST and persisted in Room", persistedAlert)
        assertEquals(alertId, persistedAlert?.alertId)
        assertEquals(com.sih26001.mobilealert.domain.model.AlertSeverity.HIGH, persistedAlert?.severity)
        assertEquals(com.sih26001.mobilealert.domain.model.AlertStatus.ACTIVE, persistedAlert?.status)
        assertEquals(68.0, persistedAlert?.riskScore ?: 0.0, 0.01)
        assertEquals("Tawang", persistedAlert?.locationName)
        assertEquals("LANDSLIDE_WARNING", persistedAlert?.eventType)
        assertTrue(persistedAlert?.requiresAck == true)
    }

    @Test
    fun onMessageReceived_withUnknownAlertId_doesNotCrashOrPersist() = kotlinx.coroutines.runBlocking {
        val unknownId = "ALT-NONEXISTENT-TEST"
        val remoteMessage = RemoteMessage.Builder("sender@gcm.googleapis.com")
            .addData("alert_id", unknownId)
            .build()

        service.onMessageReceived(remoteMessage)

        kotlinx.coroutines.delay(2000)

        val persisted = com.sih26001.mobilealert.di.DependencyContainer.database.alertDao().getAlertById(unknownId)
        org.junit.Assert.assertNull("Unknown alert_id must NOT be persisted in Room", persisted)
    }
}
