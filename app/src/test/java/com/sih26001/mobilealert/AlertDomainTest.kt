package com.sih26001.mobilealert

import com.sih26001.mobilealert.data.mapper.AlertMapper
import com.sih26001.mobilealert.data.remote.dto.AlertDto
import com.sih26001.mobilealert.data.remote.dto.LocationDto
import com.sih26001.mobilealert.data.remote.validation.AlertValidator
import com.sih26001.mobilealert.data.repository.AlertRepositoryImpl
import com.sih26001.mobilealert.domain.model.AffectedAsset
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.Location
import com.sih26001.mobilealert.domain.usecase.GetActiveAlertsUseCase
import com.sih26001.mobilealert.domain.usecase.GetAlertHistoryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AlertDomainTest {

    // 1. Valid CRITICAL alert can be represented
    @Test
    fun validCriticalAlert_canBeRepresented() {
        val issued = Instant.parse("2026-09-08T18:00:00Z")
        val expires = Instant.parse("2026-09-08T22:00:00Z")
        val alert = Alert(
            alertId = "CRIT-001",
            eventType = "landslide",
            severity = AlertSeverity.CRITICAL,
            riskScore = 0.95,
            location = Location(name = "Pass Sector 4", latitude = 30.1234, longitude = 78.5678),
            issuedAt = issued,
            expiresAt = expires,
            topDrivers = listOf("extreme_rainfall_72h", "soil_moisture_critical"),
            recommendedAction = "Immediate evacuation to high shelter",
            affectedAssets = listOf(AffectedAsset(type = "road", identifier = "NH-58")),
            source = "RISK_ENGINE_V2",
            dataQuality = "CONFIRMED",
            requiresAck = true,
            status = AlertStatus.ACTIVE
        )

        assertEquals("CRIT-001", alert.alertId)
        assertEquals(AlertSeverity.CRITICAL, alert.severity)
        assertEquals(0.95, alert.riskScore!!, 0.001)
        assertEquals(AlertStatus.ACTIVE, alert.status)
        assertEquals(true, alert.requiresAck)
        assertEquals(issued, alert.issuedAt)
        assertEquals(expires, alert.expiresAt)
    }

    // 2. NORMAL severity is accepted
    @Test
    fun normalSeverity_isAccepted() {
        val alert = Alert(
            alertId = "NORM-001",
            eventType = "landslide",
            severity = AlertSeverity.NORMAL,
            issuedAt = Instant.parse("2026-09-08T12:00:00Z"),
            status = AlertStatus.ACTIVE
        )
        assertEquals(AlertSeverity.NORMAL, alert.severity)
    }

    // 3. HIGH severity is accepted
    @Test
    fun highSeverity_isAccepted() {
        val alert = Alert(
            alertId = "HIGH-001",
            eventType = "landslide",
            severity = AlertSeverity.HIGH,
            issuedAt = Instant.parse("2026-09-08T14:00:00Z"),
            status = AlertStatus.ACTIVE
        )
        assertEquals(AlertSeverity.HIGH, alert.severity)
    }

    // 4. CRITICAL severity is accepted
    @Test
    fun criticalSeverity_isAccepted() {
        val alert = Alert(
            alertId = "CRIT-002",
            eventType = "landslide",
            severity = AlertSeverity.CRITICAL,
            issuedAt = Instant.parse("2026-09-08T16:00:00Z"),
            status = AlertStatus.ACTIVE
        )
        assertEquals(AlertSeverity.CRITICAL, alert.severity)
    }

    // 5. Missing risk score remains null (NEVER fabricated as 0)
    @Test
    fun missingRiskScore_remainsNull() {
        val alert = Alert(
            alertId = "ALT-NORISK",
            eventType = "landslide",
            severity = AlertSeverity.NORMAL,
            riskScore = null,
            issuedAt = Instant.parse("2026-09-08T10:00:00Z"),
            status = AlertStatus.ACTIVE
        )
        assertNull(alert.riskScore)
    }

    // 6. Invalid/malformed required data is safely rejected by validator
    @Test
    fun invalidRequiredData_isRejectedByValidator() {
        // Missing alert_id
        val missingIdDto = AlertDto(
            alert_id = null,
            event_type = "landslide",
            severity = "CRITICAL",
            issued_at = "2026-09-08T12:00:00Z"
        )
        val missingIdResult = AlertValidator.validate(missingIdDto)
        assertTrue(missingIdResult is AlertValidator.Result.Invalid)

        // Invalid severity
        val invalidSeverityDto = AlertDto(
            alert_id = "ALT-001",
            event_type = "landslide",
            severity = "CATASTROPHIC_UNKNOWN",
            issued_at = "2026-09-08T12:00:00Z"
        )
        val invalidSeverityResult = AlertValidator.validate(invalidSeverityDto)
        assertTrue(invalidSeverityResult is AlertValidator.Result.Invalid)

        // Out-of-bounds latitude
        val invalidLatDto = AlertDto(
            alert_id = "ALT-002",
            event_type = "landslide",
            severity = "HIGH",
            location = LocationDto(name = "Invalid Pole", latitude = 120.0, longitude = 78.0),
            issued_at = "2026-09-08T12:00:00Z"
        )
        val invalidLatResult = AlertValidator.validate(invalidLatDto)
        assertTrue(invalidLatResult is AlertValidator.Result.Invalid)

        // Malformed timestamp
        val malformedTimeDto = AlertDto(
            alert_id = "ALT-003",
            event_type = "landslide",
            severity = "NORMAL",
            issued_at = "NOT_A_TIMESTAMP"
        )
        val malformedTimeResult = AlertValidator.validate(malformedTimeDto)
        assertTrue(malformedTimeResult is AlertValidator.Result.Invalid)
    }

    // 7. Alert lifecycle states are represented correctly
    @Test
    fun alertLifecycleStates_areRepresentedCorrectly() {
        val expectedStates = listOf(
            AlertStatus.RECEIVED,
            AlertStatus.DISPLAYED,
            AlertStatus.ACTIVE,
            AlertStatus.SILENCED,
            AlertStatus.ACKNOWLEDGED,
            AlertStatus.EXPIRED
        )
        assertEquals(6, AlertStatus.values().size)
        assertTrue(AlertStatus.values().toList().containsAll(expectedStates))
    }

    // 8. SILENCED is distinct from ACKNOWLEDGED
    @Test
    fun silenced_isDistinctFromAcknowledged() {
        val silencedState = AlertStatus.SILENCED
        val ackedState = AlertStatus.ACKNOWLEDGED

        assertNotEquals(silencedState, ackedState)
        assertEquals("SILENCED", silencedState.name)
        assertEquals("ACKNOWLEDGED", ackedState.name)
    }

    // 9. alertId is preserved exactly
    @Test
    fun alertId_isPreservedExactly() {
        val exactId = "UUID-SIH-9988-7766-5544"
        val alert = Alert(
            alertId = exactId,
            eventType = "landslide",
            severity = AlertSeverity.HIGH,
            issuedAt = Instant.parse("2026-09-08T15:00:00Z"),
            status = AlertStatus.ACTIVE
        )
        assertEquals(exactId, alert.alertId)
    }

    // 10. ISO-8601 timestamps are handled correctly
    @Test
    fun iso8601Timestamps_handledCorrectly() {
        val isoIssuedString = "2026-09-08T18:30:45Z"
        val isoExpiresString = "2026-09-08T23:30:45Z"

        val parsedIssued = Instant.parse(isoIssuedString)
        val parsedExpires = Instant.parse(isoExpiresString)

        val alert = Alert(
            alertId = "ALT-ISO-01",
            eventType = "landslide",
            severity = AlertSeverity.NORMAL,
            issuedAt = parsedIssued,
            expiresAt = parsedExpires,
            status = AlertStatus.ACTIVE
        )

        assertEquals("2026-09-08T18:30:45Z", alert.issuedAt.toString())
        assertEquals("2026-09-08T23:30:45Z", alert.expiresAt.toString())
    }

    // Mapper verification: Missing fields preserved as null
    @Test
    fun alertMapper_preservesNullsCorrectly() {
        val dto = AlertDto(
            alert_id = "ALT-MAPPED-01",
            event_type = "landslide",
            severity = "HIGH",
            risk_score = null,
            location = null,
            issued_at = "2026-09-08T19:00:00Z",
            expires_at = null,
            top_drivers = null,
            recommended_action = null,
            affected_assets = null,
            source = null,
            data_quality = null,
            requires_ack = null,
            status = "ACTIVE"
        )

        val alert = AlertMapper.toDomain(dto)

        assertEquals("ALT-MAPPED-01", alert.alertId)
        assertEquals(AlertSeverity.HIGH, alert.severity)
        assertNull(alert.riskScore)
        assertNull(alert.location)
        assertNull(alert.expiresAt)
        assertNull(alert.topDrivers)
        assertNull(alert.recommendedAction)
        assertNull(alert.affectedAssets)
        assertNull(alert.source)
        assertNull(alert.dataQuality)
        assertEquals(false, alert.requiresAck)
    }

    // Repository empty flows
    @Test
    fun alertRepositoryImpl_returnsEmptyActiveAlertsInitially() = runBlocking {
        val fakeApi = object : com.sih26001.mobilealert.data.remote.api.AlertApiService {
            override suspend fun getAlerts() = emptyList<AlertDto>()
        }
        val fakeDao = object : com.sih26001.mobilealert.data.local.AlertDao {
            override fun observeAllAlerts() = kotlinx.coroutines.flow.flowOf(emptyList<com.sih26001.mobilealert.data.local.AlertEntity>())
            override fun observeActiveAlerts(activeStatuses: List<String>) = kotlinx.coroutines.flow.flowOf(emptyList<com.sih26001.mobilealert.data.local.AlertEntity>())
            override fun observeAlertById(id: String) = kotlinx.coroutines.flow.flowOf(null)
            override fun getAlertById(id: String): com.sih26001.mobilealert.data.local.AlertEntity? = null
            override fun insertAlerts(alerts: List<com.sih26001.mobilealert.data.local.AlertEntity>) {}
            override fun insertAlert(alert: com.sih26001.mobilealert.data.local.AlertEntity) {}
            override fun updateStatus(id: String, status: AlertStatus) {}
            override fun updateAcknowledgedAt(id: String, timestamp: Instant) {}
        }
        val fakePendingAckDao = object : com.sih26001.mobilealert.data.local.PendingAckDao {
            override fun observePendingAcks() = kotlinx.coroutines.flow.flowOf(emptyList<com.sih26001.mobilealert.data.local.PendingAckEntity>())
            override fun getPendingAcks() = emptyList<com.sih26001.mobilealert.data.local.PendingAckEntity>()
            override fun getEligiblePendingAcks() = emptyList<com.sih26001.mobilealert.data.local.PendingAckEntity>()
            override fun getPendingAckById(alertId: String): com.sih26001.mobilealert.data.local.PendingAckEntity? = null
            override fun observeAckSyncStatus(alertId: String) = kotlinx.coroutines.flow.flowOf(null)
            override fun insertOrIgnore(entity: com.sih26001.mobilealert.data.local.PendingAckEntity) = 1L
            override fun update(entity: com.sih26001.mobilealert.data.local.PendingAckEntity) {}
            override fun delete(alertId: String) {}
        }
        val repository = AlertRepositoryImpl(fakeApi, fakeDao, fakePendingAckDao)
        val useCase = GetActiveAlertsUseCase(repository)

        val activeAlerts = useCase().first()
        assertTrue(activeAlerts.isEmpty())
    }

    @Test
    fun alertRepositoryImpl_returnsEmptyAlertHistoryInitially() = runBlocking {
        val fakeApi = object : com.sih26001.mobilealert.data.remote.api.AlertApiService {
            override suspend fun getAlerts() = emptyList<AlertDto>()
        }
        val fakeDao = object : com.sih26001.mobilealert.data.local.AlertDao {
            override fun observeAllAlerts() = kotlinx.coroutines.flow.flowOf(emptyList<com.sih26001.mobilealert.data.local.AlertEntity>())
            override fun observeActiveAlerts(activeStatuses: List<String>) = kotlinx.coroutines.flow.flowOf(emptyList<com.sih26001.mobilealert.data.local.AlertEntity>())
            override fun observeAlertById(id: String) = kotlinx.coroutines.flow.flowOf(null)
            override fun getAlertById(id: String): com.sih26001.mobilealert.data.local.AlertEntity? = null
            override fun insertAlerts(alerts: List<com.sih26001.mobilealert.data.local.AlertEntity>) {}
            override fun insertAlert(alert: com.sih26001.mobilealert.data.local.AlertEntity) {}
            override fun updateStatus(id: String, status: AlertStatus) {}
            override fun updateAcknowledgedAt(id: String, timestamp: Instant) {}
        }
        val fakePendingAckDao = object : com.sih26001.mobilealert.data.local.PendingAckDao {
            override fun observePendingAcks() = kotlinx.coroutines.flow.flowOf(emptyList<com.sih26001.mobilealert.data.local.PendingAckEntity>())
            override fun getPendingAcks() = emptyList<com.sih26001.mobilealert.data.local.PendingAckEntity>()
            override fun getEligiblePendingAcks() = emptyList<com.sih26001.mobilealert.data.local.PendingAckEntity>()
            override fun getPendingAckById(alertId: String): com.sih26001.mobilealert.data.local.PendingAckEntity? = null
            override fun observeAckSyncStatus(alertId: String) = kotlinx.coroutines.flow.flowOf(null)
            override fun insertOrIgnore(entity: com.sih26001.mobilealert.data.local.PendingAckEntity) = 1L
            override fun update(entity: com.sih26001.mobilealert.data.local.PendingAckEntity) {}
            override fun delete(alertId: String) {}
        }
        val repository = AlertRepositoryImpl(fakeApi, fakeDao, fakePendingAckDao)
        val useCase = GetAlertHistoryUseCase(repository)

        val historyAlerts = useCase().first()
        assertTrue(historyAlerts.isEmpty())
    }
}
