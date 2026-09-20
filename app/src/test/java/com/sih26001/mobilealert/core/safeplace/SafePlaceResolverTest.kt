package com.sih26001.mobilealert.core.safeplace

import com.sih26001.mobilealert.domain.model.AffectedAsset
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SafePlaceResolverTest {

    @Test
    fun `null alert resolves to default demo destination with isDemo true`() {
        val dest = SafePlaceResolver.resolveDestination(null)

        assertNotNull(dest)
        assertTrue(dest.isDemo)
        assertEquals("Designated Emergency Relief Centre", dest.name)
        assertEquals("1.8 km", dest.distance)
        assertEquals("Approx. 7 min", dest.estimatedTime)
        assertEquals(25.5788, dest.latitude!!, 0.0001)
        assertEquals(91.8933, dest.longitude!!, 0.0001)
    }

    @Test
    fun `alert with authoritative shelter asset takes precedence over demo fallback`() {
        val alert = createAlert(
            locationName = "East Khasi Hills",
            affectedAssets = listOf(
                AffectedAsset(
                    identifier = "Civil Hospital Shelter Camp A",
                    type = "shelter"
                )
            )
        )

        val dest = SafePlaceResolver.resolveDestination(alert)

        assertNotNull(dest)
        assertFalse("Authoritative asset must have isDemo = false", dest.isDemo)
        assertEquals("Civil Hospital Shelter Camp A", dest.name)
        assertEquals("Nearby", dest.distance)
        assertEquals("Immediate", dest.estimatedTime)
        assertEquals("Verified Official Emergency Shelter", dest.statusDescription)
    }

    @Test
    fun `alert with safe_zone or relief_center asset resolves as authoritative`() {
        val alert = createAlert(
            locationName = "Tawang",
            affectedAssets = listOf(
                AffectedAsset(
                    identifier = "Tawang Higher Secondary Safe Zone",
                    type = "safe_zone"
                )
            )
        )

        val dest = SafePlaceResolver.resolveDestination(alert)

        assertFalse(dest.isDemo)
        assertEquals("Tawang Higher Secondary Safe Zone", dest.name)
    }

    @Test
    fun `alert matching Shillong without real assets resolves documented Shillong demo centre`() {
        val alert = createAlert(locationName = "Shillong, East Khasi Hills")

        val dest = SafePlaceResolver.resolveDestination(alert)

        assertTrue(dest.isDemo)
        assertEquals("Shillong Community Relief Centre", dest.name)
        assertEquals("1.8 km", dest.distance)
        assertEquals("Approx. 7 min", dest.estimatedTime)
        assertEquals(25.5788, dest.latitude!!, 0.0001)
        assertEquals(91.8933, dest.longitude!!, 0.0001)
    }

    @Test
    fun `alert matching Tawang without real assets resolves documented Tawang demo centre`() {
        val alert = createAlert(locationName = "Tawang Sector 3")

        val dest = SafePlaceResolver.resolveDestination(alert)

        assertTrue(dest.isDemo)
        assertEquals("Tawang Community Relief Centre", dest.name)
        assertEquals("2.3 km", dest.distance)
        assertEquals("Approx. 10 min", dest.estimatedTime)
        assertEquals(27.5860, dest.latitude!!, 0.0001)
        assertEquals(91.8650, dest.longitude!!, 0.0001)
    }

    @Test
    fun `alert in other district creates named demo centre`() {
        val alert = createAlert(locationName = "Guwahati West")

        val dest = SafePlaceResolver.resolveDestination(alert)

        assertTrue(dest.isDemo)
        assertEquals("Guwahati West Community Relief Centre", dest.name)
        assertEquals("1.8 km", dest.distance)
        assertEquals("Approx. 7 min", dest.estimatedTime)
    }

    private fun createAlert(
        locationName: String,
        affectedAssets: List<AffectedAsset>? = null
    ): Alert {
        return Alert(
            alertId = "ALT-DEMO-TEST",
            eventType = "landslide",
            severity = AlertSeverity.HIGH,
            status = AlertStatus.ACTIVE,
            location = Location(
                name = locationName,
                latitude = 25.5788,
                longitude = 91.8933
            ),
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            recommendedAction = "Evacuate immediately",
            source = "sih26001_demo",
            affectedAssets = affectedAssets
        )
    }
}
