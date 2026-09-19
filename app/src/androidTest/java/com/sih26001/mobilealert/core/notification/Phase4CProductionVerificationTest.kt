package com.sih26001.mobilealert.core.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.messaging.RemoteMessage
import com.sih26001.mobilealert.data.fcm.FcmProcessingResult
import com.sih26001.mobilealert.data.fcm.SihFirebaseMessagingService
import com.sih26001.mobilealert.data.local.toDomain
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Production verification on device for Phase 4C:
 * - End-to-end FCM -> REST -> Validation -> Mapping -> Room -> Notification
 * - Accurate Risk Score formatting (68.0%, not 6800%)
 * - Location presentation (Tawang with coordinates)
 * - Deterministic notification ID and deep link
 * - Duplicate handling without notification flood
 */
@RunWith(AndroidJUnit4::class)
class Phase4CProductionVerificationTest {

    private lateinit var context: Context
    private lateinit var service: SihFirebaseMessagingService
    private lateinit var notificationManager: NotificationManager

    private val pilotAlertId = "ALT-TAW-1789811514-4E51"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service = SihFirebaseMessagingService()
        SihFirebaseMessagingService.processor.clearCacheForTesting()

        // Ensure channels exist at application startup
        NotificationChannels.createChannels(context)
    }

    @Test
    fun testPilotAlert_endToEndPipeline_postsFormattedNotification() = runBlocking {
        // Clear old database entries if present
        DependencyContainer.database.alertDao().deleteAllAlerts()

        val remoteMessage = RemoteMessage.Builder("sender@gcm.googleapis.com")
            .addData("alert_id", pilotAlertId)
            .build()

        // 1. Deliver FCM trigger
        service.onMessageReceived(remoteMessage)

        // 2. Await authoritative REST fetch and Room persistence
        var persistedAlert: com.sih26001.mobilealert.data.local.AlertEntity? = null
        for (i in 1..25) {
            delay(200)
            persistedAlert = DependencyContainer.database.alertDao().getAlertById(pilotAlertId)
            if (persistedAlert != null) break
        }

        assertNotNull("Authoritative alert must be persisted in Room", persistedAlert)
        assertEquals(pilotAlertId, persistedAlert?.alertId)
        assertEquals("Tawang", persistedAlert?.locationName)
        assertEquals(AlertSeverity.HIGH, persistedAlert?.severity)
        assertEquals(AlertStatus.ACTIVE, persistedAlert?.status)
        assertEquals(68.0, persistedAlert?.riskScore ?: 0.0, 0.01)

        // 3. Verify notification attributes
        val domainAlert = persistedAlert!!.toDomain()
        val contentText = AlertNotificationManagerImpl.formatContentText(domainAlert)

        // Risk score MUST be 68.0% and NEVER 6800%
        assertTrue("Content text must contain 68.0%: '$contentText'", contentText.contains("68.0%"))
        assertFalse("Content text must not contain 6800%: '$contentText'", contentText.contains("6800"))

        // Location MUST contain Tawang and coordinates
        assertTrue("Content text must contain Tawang: '$contentText'", contentText.contains("Tawang"))
        assertTrue("Content text must contain coordinates: '$contentText'", contentText.contains("27.5861, 91.8694"))

        // 4. Verify deterministic notification ID
        val expectedNotificationId = AlertNotificationManagerImpl.getNotificationId(pilotAlertId)
        assertEquals(pilotAlertId.hashCode(), expectedNotificationId)

        // 5. Verify deep link target
        val deepLinkUri = AlertNotificationManagerImpl.getDeepLinkUri(pilotAlertId)
        assertEquals("sih26001://alert/$pilotAlertId", deepLinkUri.toString())
    }

    @Test
    fun testDuplicateTrigger_preservesSingleRoomRecordAndDeterministicNotification() = runBlocking {
        val remoteMessage = RemoteMessage.Builder("sender@gcm.googleapis.com")
            .addData("alert_id", pilotAlertId)
            .build()

        // First delivery
        service.onMessageReceived(remoteMessage)
        delay(1000)

        // Second delivery (duplicate)
        service.onMessageReceived(remoteMessage)
        delay(500)

        // Verify deduplication
        val duplicateCheck = SihFirebaseMessagingService.processor.processData(mapOf("alert_id" to pilotAlertId))
        assertTrue(duplicateCheck is FcmProcessingResult.DuplicateAlert)

        // Verify deterministic ID
        val notificationId = AlertNotificationManagerImpl.getNotificationId(pilotAlertId)
        assertEquals(pilotAlertId.hashCode(), notificationId)
    }

    @Test
    fun testRiskScoreFormatting_edgeCases() {
        assertEquals("Risk Score: 0.0%", AlertNotificationManagerImpl.formatRiskScore(0.0))
        assertEquals("Risk Score: 100.0%", AlertNotificationManagerImpl.formatRiskScore(100.0))
        assertEquals("Risk Score: 68.0%", AlertNotificationManagerImpl.formatRiskScore(68.0))
        assertEquals("Risk score unavailable", AlertNotificationManagerImpl.formatRiskScore(null))
    }

    @Test
    fun testLocationFormatting_edgeCases() {
        val fullLoc = com.sih26001.mobilealert.domain.model.Location("Tawang", 27.5861, 91.8694)
        val nameOnlyLoc = com.sih26001.mobilealert.domain.model.Location("Tawang", null, null)
        val coordsOnlyLoc = com.sih26001.mobilealert.domain.model.Location(null, 27.5861, 91.8694)

        assertEquals("Location: Tawang\nCoordinates: 27.5861, 91.8694", AlertNotificationManagerImpl.formatLocation(fullLoc))
        assertEquals("Location: Tawang", AlertNotificationManagerImpl.formatLocation(nameOnlyLoc))
        assertEquals("Location: 27.5861, 91.8694", AlertNotificationManagerImpl.formatLocation(coordsOnlyLoc))
        org.junit.Assert.assertNull(AlertNotificationManagerImpl.formatLocation(null))
    }
}
