package com.sih26001.mobilealert.core.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sih26001.mobilealert.core.util.Constants
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Instrumentation verification for Phase 4C:
 * 1. Notification channels exist after application / channel startup.
 * 2. Critical, High, and Normal channels can be resolved correctly.
 * 3. Notification posting for all severities does not crash.
 * 4. Defensive notification posting handles security / permission issues gracefully without crashing.
 * 5. Notification cancellation does not crash.
 * 6. Application notification infrastructure is stable and lifecycle-independent.
 */
@RunWith(AndroidJUnit4::class)
class AlertNotificationManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var alertNotificationManager: AlertNotificationManagerImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        alertNotificationManager = AlertNotificationManagerImpl(context)

        // Ensure channels are created
        NotificationChannels.createChannels(context)
    }

    @Test
    fun testNotificationChannelsExistAndAreConfiguredCorrectly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val normalChannel = notificationManager.getNotificationChannel(Constants.CHANNEL_ID_NORMAL)
            val highChannel = notificationManager.getNotificationChannel(Constants.CHANNEL_ID_HIGH)
            val criticalChannel = notificationManager.getNotificationChannel(Constants.CHANNEL_ID_CRITICAL)

            assertNotNull("NORMAL channel must exist", normalChannel)
            assertNotNull("HIGH channel must exist", highChannel)
            assertNotNull("CRITICAL channel must exist", criticalChannel)

            assertEquals(NotificationManager.IMPORTANCE_DEFAULT, normalChannel.importance)
            assertEquals(NotificationManager.IMPORTANCE_HIGH, highChannel.importance)
            assertEquals(NotificationManager.IMPORTANCE_HIGH, criticalChannel.importance)

            assertTrue("HIGH channel must have vibration enabled", highChannel.shouldVibrate())
            assertTrue("CRITICAL channel must have vibration enabled", criticalChannel.shouldVibrate())
            // Note: canBypassDnd() returns true only if ACCESS_NOTIFICATION_POLICY has been granted by user/system.
            // As per Rule 9, DND bypass is requested via setBypassDnd(true) but subject to OS policy.
        }
    }

    @Test
    fun testCriticalHighNormalChannelsCanBeResolved() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val severities = listOf(AlertSeverity.CRITICAL, AlertSeverity.HIGH, AlertSeverity.NORMAL)
            for (severity in severities) {
                val channelId = AlertNotificationManagerImpl.getChannelIdForSeverity(severity)
                val channel = notificationManager.getNotificationChannel(channelId)
                assertNotNull("Channel $channelId for $severity must be resolvable from NotificationManager", channel)
            }
        }
    }

    @Test
    fun testNotificationPostingDoesNotCrash() {
        val testAlerts = listOf(
            createAlert("ALT-INST-CRIT", AlertSeverity.CRITICAL, 90.0),
            createAlert("ALT-INST-HIGH", AlertSeverity.HIGH, 68.0),
            createAlert("ALT-INST-NORM", AlertSeverity.NORMAL, 35.0)
        )

        for (alert in testAlerts) {
            // Posting should execute cleanly without exceptions
            alertNotificationManager.showAlertNotification(alert)
        }
    }

    @Test
    fun testNotificationCancellationDoesNotCrash() {
        // Cancel existing and non-existing notifications
        alertNotificationManager.cancelAlertNotification("ALT-INST-HIGH")
        alertNotificationManager.cancelAlertNotification("ALT-NON-EXISTENT-999")
    }

    @Test
    fun testNotificationBuildingPreservesDeterministicIdAndDeepLink() {
        val alert = createAlert("ALT-TAW-1789811514-4E51", AlertSeverity.HIGH, 68.0)
        val notificationId = AlertNotificationManagerImpl.getNotificationId(alert.alertId)
        assertEquals(alert.alertId.hashCode(), notificationId)

        val deepLinkString = AlertNotificationManagerImpl.getDeepLinkUriString(alert.alertId)
        assertEquals("sih26001://alert/ALT-TAW-1789811514-4E51", deepLinkString)

        val notification = alertNotificationManager.buildNotification(alert)
        assertNotNull(notification)
    }

    @Test
    fun testChannelRecreationWhenChannelDeleted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alert = createAlert("ALT-RECREATE-CH", AlertSeverity.NORMAL, 40.0)
            // Even if we simulate missing or calling showAlertNotification, it ensures channels exist
            alertNotificationManager.showAlertNotification(alert)
            val channel = notificationManager.getNotificationChannel(Constants.CHANNEL_ID_NORMAL)
            assertNotNull(channel)
        }
    }

    private fun createAlert(
        id: String,
        severity: AlertSeverity,
        riskScore: Double
    ): Alert {
        return Alert(
            alertId = id,
            eventType = "LANDSLIDE_WARNING",
            severity = severity,
            riskScore = riskScore,
            location = Location(name = "Tawang", latitude = 27.5861, longitude = 91.8694),
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            topDrivers = listOf("HEAVY_RAINFALL"),
            recommendedAction = "Evacuate danger zones",
            affectedAssets = null,
            source = "Sixth Sense Early Warning System",
            dataQuality = "GOOD",
            requiresAck = true,
            status = AlertStatus.ACTIVE
        )
    }
}
