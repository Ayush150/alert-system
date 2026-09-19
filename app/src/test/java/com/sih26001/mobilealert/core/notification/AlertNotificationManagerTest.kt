package com.sih26001.mobilealert.core.notification

import androidx.core.app.NotificationCompat
import com.sih26001.mobilealert.core.util.Constants
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AlertNotificationManagerTest {

    private val sampleAlertId = "ALT-TAW-1789811514-4E51"

    // ------------------------------------------------------------------------
    // Test 1 — CRITICAL channel & priority
    // ------------------------------------------------------------------------
    @Test
    fun `critical alert maps to CRITICAL channel and MAX priority`() {
        val channelId = AlertNotificationManagerImpl.getChannelIdForSeverity(AlertSeverity.CRITICAL)
        val priority = AlertNotificationManagerImpl.getPriorityForSeverity(AlertSeverity.CRITICAL)

        assertEquals(Constants.CHANNEL_ID_CRITICAL, channelId)
        assertEquals(NotificationCompat.PRIORITY_MAX, priority)
    }

    // ------------------------------------------------------------------------
    // Test 2 — HIGH channel & priority
    // ------------------------------------------------------------------------
    @Test
    fun `high alert maps to HIGH channel and HIGH priority`() {
        val channelId = AlertNotificationManagerImpl.getChannelIdForSeverity(AlertSeverity.HIGH)
        val priority = AlertNotificationManagerImpl.getPriorityForSeverity(AlertSeverity.HIGH)

        assertEquals(Constants.CHANNEL_ID_HIGH, channelId)
        assertEquals(NotificationCompat.PRIORITY_HIGH, priority)
    }

    // ------------------------------------------------------------------------
    // Test 3 — NORMAL channel & priority
    // ------------------------------------------------------------------------
    @Test
    fun `normal alert maps to NORMAL channel and DEFAULT priority`() {
        val channelId = AlertNotificationManagerImpl.getChannelIdForSeverity(AlertSeverity.NORMAL)
        val priority = AlertNotificationManagerImpl.getPriorityForSeverity(AlertSeverity.NORMAL)

        assertEquals(Constants.CHANNEL_ID_NORMAL, channelId)
        assertEquals(NotificationCompat.PRIORITY_DEFAULT, priority)
    }

    // ------------------------------------------------------------------------
    // Test 4 — Risk score formatting
    // Given risk_score = 68.0, verify 68.0% and NOT 6800%
    // ------------------------------------------------------------------------
    @Test
    fun `risk score 68_0 formats as 68_0 percent and not 6800 percent`() {
        val riskText = AlertNotificationManagerImpl.formatRiskScore(68.0)

        assertTrue("Expected 68.0% in '$riskText'", riskText.contains("68.0%"))
        assertFalse("Must NOT contain 6800% in '$riskText'", riskText.contains("6800"))

        val alert = createTestAlert(riskScore = 68.0)
        val contentText = AlertNotificationManagerImpl.formatContentText(alert)
        assertTrue("Content text must contain 68.0%: '$contentText'", contentText.contains("68.0%"))
        assertFalse("Content text must not contain 6800%: '$contentText'", contentText.contains("6800"))
    }

    @Test
    fun `null risk score formats safely as unavailable`() {
        val riskText = AlertNotificationManagerImpl.formatRiskScore(null)
        assertEquals("Risk score unavailable", riskText)
    }

    // ------------------------------------------------------------------------
    // Test 5 — Location formatting
    // Given location = Tawang, lat = 27.5861, lon = 91.8694
    // ------------------------------------------------------------------------
    @Test
    fun `location with name and coordinates formats with authoritative name and coordinates`() {
        val location = Location(name = "Tawang", latitude = 27.5861, longitude = 91.8694)
        val formatted = AlertNotificationManagerImpl.formatLocation(location)

        assertTrue(formatted!!.contains("Location: Tawang"))
        assertTrue(formatted.contains("Coordinates: 27.5861, 91.8694"))

        val alert = createTestAlert(location = location)
        val contentText = AlertNotificationManagerImpl.formatContentText(alert)
        assertTrue(contentText.contains("Tawang"))
        assertTrue(contentText.contains("27.5861, 91.8694"))
    }

    @Test
    fun `location with coordinates only falls back safely to coordinate display`() {
        val location = Location(name = null, latitude = 27.5861, longitude = 91.8694)
        val formatted = AlertNotificationManagerImpl.formatLocation(location)

        assertEquals("Location: 27.5861, 91.8694", formatted)
    }

    @Test
    fun `location with name only formats without coordinates`() {
        val location = Location(name = "Tawang", latitude = null, longitude = null)
        val formatted = AlertNotificationManagerImpl.formatLocation(location)

        assertEquals("Location: Tawang", formatted)
    }

    @Test
    fun `null location returns null format`() {
        val formatted = AlertNotificationManagerImpl.formatLocation(null)
        assertNull(formatted)
    }

    // ------------------------------------------------------------------------
    // Test 6 — Deterministic notification ID
    // ------------------------------------------------------------------------
    @Test
    fun `deterministic notification ID uses alertId hashCode consistently`() {
        val alert = createTestAlert(id = sampleAlertId)
        val expectedId = sampleAlertId.hashCode()

        assertEquals(expectedId, AlertNotificationManagerImpl.getNotificationId(alert.alertId))
        assertEquals(expectedId, AlertNotificationManagerImpl.getNotificationId(sampleAlertId))
        // Multiple evaluations produce the exact same ID
        assertEquals(
            AlertNotificationManagerImpl.getNotificationId(sampleAlertId),
            AlertNotificationManagerImpl.getNotificationId(sampleAlertId)
        )
    }

    // ------------------------------------------------------------------------
    // Test 7 — Deep link
    // ------------------------------------------------------------------------
    @Test
    fun `deep link uri targets sih26001 alert protocol with correct alertId`() {
        val deepLinkString = AlertNotificationManagerImpl.getDeepLinkUriString(sampleAlertId)
        assertEquals("sih26001://alert/$sampleAlertId", deepLinkString)
    }

    // ------------------------------------------------------------------------
    // Test 8 — Cancellation uses deterministic ID
    // ------------------------------------------------------------------------
    @Test
    fun `cancel notification uses the same deterministic notification ID`() {
        val alertId = "ALT-TAW-1789811514-4E51"
        val expectedNotificationId = alertId.hashCode()

        assertEquals(expectedNotificationId, AlertNotificationManagerImpl.getNotificationId(alertId))
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private fun createTestAlert(
        id: String = sampleAlertId,
        severity: AlertSeverity = AlertSeverity.HIGH,
        status: AlertStatus = AlertStatus.ACTIVE,
        riskScore: Double? = 68.0,
        location: Location? = Location("Tawang", 27.5861, 91.8694)
    ): Alert {
        return Alert(
            alertId = id,
            eventType = "LANDSLIDE_WARNING",
            severity = severity,
            riskScore = riskScore,
            location = location,
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            topDrivers = listOf("HEAVY_RAINFALL"),
            recommendedAction = "Evacuate low-lying areas",
            affectedAssets = null,
            source = "Sixth Sense Early Warning System",
            dataQuality = "GOOD",
            requiresAck = true,
            status = status
        )
    }
}
