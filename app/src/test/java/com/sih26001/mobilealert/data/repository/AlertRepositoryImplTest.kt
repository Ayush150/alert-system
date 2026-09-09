package com.sih26001.mobilealert.data.repository

import com.sih26001.mobilealert.data.remote.api.AlertApiService
import com.sih26001.mobilealert.data.remote.dto.AlertDto
import com.sih26001.mobilealert.data.remote.dto.LocationDto
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.data.local.AlertDao
import com.sih26001.mobilealert.data.local.AlertEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

class AlertRepositoryImplTest {

    private lateinit var repository: AlertRepositoryImpl
    private lateinit var mockApiService: FakeAlertApiService
    private lateinit var mockDao: FakeAlertDao

    @Before
    fun setup() {
        mockApiService = FakeAlertApiService()
        mockDao = FakeAlertDao()
        repository = AlertRepositoryImpl(mockApiService, mockDao)
    }

    @Test
    fun `refreshAlerts with valid DTOs populates cache successfully`() = runTest {
        mockApiService.mockResponse = listOf(
            createMockDto("ALT-1", "CRITICAL", "ACTIVE"),
            createMockDto("ALT-2", "HIGH", "ACTIVE"),
            createMockDto("ALT-3", "NORMAL", "ACTIVE")
        )

        val result = repository.refreshAlerts()
        assertTrue(result.isSuccess)

        val active = repository.getActiveAlerts().first()
        assertEquals(3, active.size)
    }

    @Test
    fun `refreshAlerts handles empty response without error`() = runTest {
        mockApiService.mockResponse = emptyList()

        val result = repository.refreshAlerts()
        assertTrue(result.isSuccess)

        val active = repository.getActiveAlerts().first()
        assertTrue(active.isEmpty())
    }

    @Test
    fun `refreshAlerts rejects invalid DTO and retains valid DTO`() = runTest {
        mockApiService.mockResponse = listOf(
            createMockDto("ALT-1", "CRITICAL", "ACTIVE"), // Valid
            AlertDto(alert_id = "ALT-BAD") // Invalid, missing fields
        )

        val result = repository.refreshAlerts()
        assertTrue(result.isSuccess)

        val active = repository.getActiveAlerts().first()
        assertEquals(1, active.size)
        assertEquals("ALT-1", active[0].alertId)
    }

    @Test
    fun `refreshAlerts retains previous cache on IOException`() = runTest {
        // First successful fetch
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))
        repository.refreshAlerts()
        
        // Second fetch fails
        mockApiService.shouldThrow = IOException("Network Error")
        val result = repository.refreshAlerts()
        
        assertTrue(result.isFailure)
        
        // Verify cache retained
        val active = repository.getActiveAlerts().first()
        assertEquals(1, active.size)
        assertEquals("ALT-1", active[0].alertId)
    }

    @Test
    fun `getAlertById returns correct alert or null`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))
        repository.refreshAlerts()

        val found = repository.getAlertById("ALT-1").first()
        assertNotNull(found)
        assertEquals("ALT-1", found?.alertId)

        val notFound = repository.getAlertById("UNKNOWN").first()
        assertNull(notFound)
    }

    @Test
    fun `active filtering matches expected status`() = runTest {
        mockApiService.mockResponse = listOf(
            createMockDto("ALT-1", "CRITICAL", "ACTIVE"),
            createMockDto("ALT-2", "HIGH", "ACKNOWLEDGED")
        )
        repository.refreshAlerts()

        val active = repository.getActiveAlerts().first()
        assertEquals(1, active.size)
        assertEquals("ALT-1", active[0].alertId)
    }

    @Test
    fun `history filtering matches expected status`() = runTest {
        mockApiService.mockResponse = listOf(
            createMockDto("ALT-1", "CRITICAL", "ACTIVE"),
            createMockDto("ALT-2", "HIGH", "ACKNOWLEDGED"),
            createMockDto("ALT-3", "NORMAL", "EXPIRED")
        )
        repository.refreshAlerts()

        val history = repository.getAlertHistory().first()
        assertEquals(2, history.size)
        assertTrue(history.any { it.alertId == "ALT-2" })
        assertTrue(history.any { it.alertId == "ALT-3" })
    }

    @Test
    fun `acknowledgeAlert updates status and retains alert`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))
        repository.refreshAlerts()

        val ackResult = repository.acknowledgeAlert("ALT-1")
        assertTrue(ackResult.isSuccess)

        val active = repository.getActiveAlerts().first()
        assertTrue(active.isEmpty())

        val history = repository.getAlertHistory().first()
        assertEquals(1, history.size)
        assertEquals("ALT-1", history[0].alertId)
        assertEquals(AlertStatus.ACKNOWLEDGED, history[0].status)
    }

    @Test
    fun `silenceAlert updates status but does not acknowledge`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))
        repository.refreshAlerts()

        val silenceResult = repository.silenceAlert("ALT-1")
        assertTrue(silenceResult.isSuccess)

        // Silenced alerts should STILL appear in getActiveAlerts
        val active = repository.getActiveAlerts().first()
        assertEquals(1, active.size)
        assertEquals(AlertStatus.SILENCED, active[0].status)

        // It should NOT appear in history
        val history = repository.getAlertHistory().first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `duplicate refresh replaces cache without duplicates`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))
        
        repository.refreshAlerts()
        assertEquals(1, repository.getActiveAlerts().first().size)
        
        // Refresh again with identical response
        repository.refreshAlerts()
        assertEquals(1, repository.getActiveAlerts().first().size)
    }

    @Test
    fun `refreshAlert with valid alertId fetches, validates, and persists alert`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))

        val result = repository.refreshAlert("ALT-1")
        assertTrue(result.isSuccess)

        val alert = result.getOrNull()
        assertNotNull(alert)
        assertEquals("ALT-1", alert?.alertId)

        // Verify persisted in Room DAO
        val persisted = repository.getAlertById("ALT-1").first()
        assertNotNull(persisted)
        assertEquals("ALT-1", persisted?.alertId)
    }

    @Test
    fun `refreshAlert with unknown alertId fails safely`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))

        val result = repository.refreshAlert("ALT-UNKNOWN")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun `refreshAlert with malformed DTO fails validation and does not persist`() = runTest {
        mockApiService.mockResponse = listOf(AlertDto(alert_id = "ALT-BAD")) // Missing required fields

        val result = repository.refreshAlert("ALT-BAD")
        assertTrue(result.isFailure)

        // Verify not persisted
        val persisted = repository.getAlertById("ALT-BAD").first()
        assertNull(persisted)
    }

    @Test
    fun `refreshAlert with expired alert sets status EXPIRED`() = runTest {
        val expiredDto = AlertDto(
            alert_id = "ALT-EXP",
            event_type = "LANDSLIDE_RISK",
            severity = "HIGH",
            risk_score = 75.0,
            location = LocationDto("Test", 10.0, 20.0),
            issued_at = "2020-01-01T00:00:00Z",
            expires_at = "2020-01-01T01:00:00Z", // Past date
            top_drivers = listOf("Rain"),
            recommended_action = "Stay alert",
            affected_assets = emptyList(),
            source = "Test",
            data_quality = "GOOD",
            requires_ack = false,
            status = "ACTIVE"
        )
        mockApiService.mockResponse = listOf(expiredDto)

        val result = repository.refreshAlert("ALT-EXP")
        assertTrue(result.isSuccess)
        assertEquals(AlertStatus.EXPIRED, result.getOrNull()?.status)
    }

    @Test
    fun `refreshAlert preserves null riskScore and never defaults to zero`() = runTest {
        val dtoWithNullRisk = AlertDto(
            alert_id = "ALT-NULL-RISK",
            event_type = "LANDSLIDE_RISK",
            severity = "NORMAL",
            risk_score = null, // Missing risk score
            location = LocationDto("Test", 10.0, 20.0),
            issued_at = "2026-09-08T10:15:00Z",
            expires_at = "2026-09-08T11:15:00Z",
            top_drivers = null,
            recommended_action = null,
            affected_assets = null,
            source = "Test",
            data_quality = null,
            requires_ack = false,
            status = "ACTIVE"
        )
        mockApiService.mockResponse = listOf(dtoWithNullRisk)

        val result = repository.refreshAlert("ALT-NULL-RISK")
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull()?.riskScore)
    }

    @Test
    fun `refreshAlert handles IOException without crash`() = runTest {
        mockApiService.shouldThrow = IOException("Connection reset")

        val result = repository.refreshAlert("ALT-1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    private fun createMockDto(id: String, severity: String, status: String): AlertDto {
        return AlertDto(
            alert_id = id,
            event_type = "LANDSLIDE_RISK",
            severity = severity,
            risk_score = 80.0,
            location = LocationDto("Test", 10.0, 20.0),
            issued_at = "2026-09-08T10:15:00Z",
            expires_at = "2026-09-08T11:15:00Z",
            top_drivers = listOf("Rain"),
            recommended_action = "Run",
            affected_assets = emptyList(),
            source = "Test",
            data_quality = "GOOD",
            requires_ack = true,
            status = status
        )
    }
}

class FakeAlertApiService : AlertApiService {
    var mockResponse: List<AlertDto> = emptyList()
    var shouldThrow: Exception? = null

    override suspend fun getAlerts(): List<AlertDto> {
        shouldThrow?.let { throw it }
        return mockResponse
    }
}

class FakeAlertDao : AlertDao {
    private val entities = MutableStateFlow<Map<String, AlertEntity>>(emptyMap())

    override fun observeAllAlerts(): Flow<List<AlertEntity>> {
        return entities.map { map ->
            map.values.sortedByDescending { it.issuedAt }
        }
    }

    override fun observeActiveAlerts(activeStatuses: List<String>): Flow<List<AlertEntity>> {
        return entities.map { map ->
            map.values.filter { activeStatuses.contains(it.status.name) }
                .sortedByDescending { it.issuedAt }
        }
    }

    override fun observeAlertById(id: String): Flow<AlertEntity?> {
        return entities.map { it[id] }
    }

    override fun insertAlerts(alerts: List<AlertEntity>) {
        val current = entities.value.toMutableMap()
        alerts.forEach { current[it.alertId] = it }
        entities.value = current
    }

    override fun insertAlert(alert: AlertEntity) {
        val current = entities.value.toMutableMap()
        current[alert.alertId] = alert
        entities.value = current
    }

    override fun updateStatus(id: String, status: AlertStatus) {
        val current = entities.value.toMutableMap()
        current[id]?.let {
            current[id] = it.copy(status = status)
            entities.value = current
        }
    }

    override fun updateAcknowledgedAt(id: String, timestamp: Instant) {
        val current = entities.value.toMutableMap()
        current[id]?.let {
            current[id] = it.copy(acknowledgedAt = timestamp)
            entities.value = current
        }
    }
}
