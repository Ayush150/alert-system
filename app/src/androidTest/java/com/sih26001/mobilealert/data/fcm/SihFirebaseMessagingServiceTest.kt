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
}
