package com.sih26001.mobilealert.core.demo

import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.DEMO_ALERT_SOURCE
import com.sih26001.mobilealert.domain.model.isDemoAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DemoProtocolManagerTest {

    @Test
    fun `initial state of DemoProtocolManager is permanently inactive`() {
        val manager = DemoProtocolManagerImpl()
        assertFalse("DemoProtocolManager must start inactive", manager.isStarted.value)
        assertEquals(DemoProtocolState.INACTIVE, manager.state.value)
    }

    @Test
    fun `local startProtocol does NOT mutate state (client transitions permanently disabled)`() {
        val manager = DemoProtocolManagerImpl()
        manager.startProtocol()
        assertFalse("DemoProtocolManager state mutation must remain disabled", manager.isStarted.value)
        assertEquals(DemoProtocolState.INACTIVE, manager.state.value)
    }

    @Test
    fun `local state transition methods are all safe no-ops`() {
        val manager = DemoProtocolManagerImpl()
        manager.setNormal()
        manager.setWarning()
        manager.setHighAlert()
        manager.setResolved()
        manager.stopProtocol()

        assertFalse(manager.isStarted.value)
        assertEquals(DemoProtocolState.INACTIVE, manager.state.value)
    }

    @Test
    fun `isDemoAlert identifies demo alerts correctly`() {
        val demoAlert1 = createAlert("ALT-DEMO-WARN", "LANDSLIDE_RISK", DEMO_ALERT_SOURCE)
        val demoAlert2 = createAlert("ALT-DEMO-CRIT", "LANDSLIDE_DANGER", "sih26001_demo")
        val demoAlert3 = createAlert("DEMO-001", "LANDSLIDE_DANGER", "custom_demo")

        assertTrue(demoAlert1.isDemoAlert())
        assertTrue(demoAlert2.isDemoAlert())
        assertTrue(demoAlert3.isDemoAlert())
    }

    @Test
    fun `isDemoAlert returns false for real cloud alerts`() {
        val realAlert1 = createAlert("ALT-TAW-1789749077-4167", "LANDSLIDE_RISK", "regional_sensor_network")
        val realAlert2 = createAlert("ALT-SHI-2026-001", "HEAVY_RAIN", "IMD")
        val realAlert3 = createAlert("ALT-GHY-9999", "FLOOD_RISK", "risk_fusion")
        val realAlert4 = createAlert("ALT-PILOT-1", "LANDSLIDE_WARNING", null)

        assertFalse(realAlert1.isDemoAlert())
        assertFalse(realAlert2.isDemoAlert())
        assertFalse(realAlert3.isDemoAlert())
        assertFalse(realAlert4.isDemoAlert())
    }

    private fun createAlert(alertId: String, eventType: String, source: String?): Alert {
        return Alert(
            alertId = alertId,
            eventType = eventType,
            severity = AlertSeverity.HIGH,
            riskScore = 0.85,
            location = null,
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            topDrivers = emptyList(),
            recommendedAction = "Take shelter",
            affectedAssets = emptyList(),
            source = source,
            dataQuality = "GOOD",
            requiresAck = false,
            status = AlertStatus.ACTIVE
        )
    }
}
