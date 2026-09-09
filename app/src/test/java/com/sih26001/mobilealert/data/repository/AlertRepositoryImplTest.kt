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
    private lateinit var mockPendingAckDao: FakePendingAckDao

    @Before
    fun setup() {
        mockApiService = FakeAlertApiService()
        mockDao = FakeAlertDao()
        mockPendingAckDao = FakePendingAckDao()
        repository = AlertRepositoryImpl(mockApiService, mockDao, mockPendingAckDao)
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
    fun `valid ACTIVE alert becomes ACKNOWLEDGED and sets acknowledgedAt and queues pending ACK`() = runTest {
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
        assertNotNull(history[0].acknowledgedAt)

        // Verify queued in Room pending_acks
        val pendingAcks = mockPendingAckDao.getPendingAcks()
        assertEquals(1, pendingAcks.size)
        assertEquals("ALT-1", pendingAcks[0].alertId)
        assertEquals(com.sih26001.mobilealert.data.local.AckSyncStatus.PENDING, pendingAcks[0].status)

        // Verify observePendingAckIds
        val pendingIds = repository.observePendingAckIds().first()
        assertTrue(pendingIds.contains("ALT-1"))
    }

    @Test
    fun `valid SILENCED alert becomes ACKNOWLEDGED`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "HIGH", "ACTIVE"))
        repository.refreshAlerts()

        // First silence it
        repository.silenceAlert("ALT-1")
        val silenced = repository.getAlertById("ALT-1").first()
        assertEquals(AlertStatus.SILENCED, silenced?.status)

        // Then acknowledge it
        val ackResult = repository.acknowledgeAlert("ALT-1")
        assertTrue(ackResult.isSuccess)

        val acknowledged = repository.getAlertById("ALT-1").first()
        assertEquals(AlertStatus.ACKNOWLEDGED, acknowledged?.status)
        assertNotNull(acknowledged?.acknowledgedAt)
    }

    @Test
    fun `blank alert ID fails safely`() = runTest {
        val resultEmpty = repository.acknowledgeAlert("")
        assertTrue(resultEmpty.isFailure)
        assertTrue(resultEmpty.exceptionOrNull() is IllegalArgumentException)

        val resultBlank = repository.acknowledgeAlert("   ")
        assertTrue(resultBlank.isFailure)
        assertTrue(resultBlank.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `unknown alert ID fails safely with NoSuchElementException`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))
        repository.refreshAlerts()

        val result = repository.acknowledgeAlert("ALT-NONEXISTENT")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun `duplicate ACK is idempotent and creates exactly one queue entry`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))
        repository.refreshAlerts()

        val ack1 = repository.acknowledgeAlert("ALT-1")
        assertTrue(ack1.isSuccess)
        assertEquals(1, mockPendingAckDao.getPendingAcks().size)

        // Second ACK call
        val ack2 = repository.acknowledgeAlert("ALT-1")
        assertTrue(ack2.isSuccess)
        // Must NOT create duplicate queue entry
        assertEquals(1, mockPendingAckDao.getPendingAcks().size)
    }

    @Test
    fun `transaction failure rolls back and returns failure without partial commit`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))
        repository.refreshAlerts()

        // Create repository with a failing transaction runner
        val failingTxRunner = object : com.sih26001.mobilealert.data.local.DatabaseTransactionRunner {
            override suspend fun <T> invoke(block: suspend () -> T): T {
                throw RuntimeException("Database disk I/O error during transaction")
            }
        }
        val repoWithFailingTx = AlertRepositoryImpl(
            apiService = mockApiService,
            alertDao = mockDao,
            pendingAckDao = mockPendingAckDao,
            transactionRunner = failingTxRunner
        )

        val ackResult = repoWithFailingTx.acknowledgeAlert("ALT-1")
        assertTrue(ackResult.isFailure)
        assertEquals("Database disk I/O error during transaction", ackResult.exceptionOrNull()?.message)

        // Verify alert remains in original state and queue is empty
        val alert = repoWithFailingTx.getAlertById("ALT-1").first()
        assertEquals(AlertStatus.ACTIVE, alert?.status)
        assertNull(alert?.acknowledgedAt)
        assertTrue(mockPendingAckDao.getPendingAcks().isEmpty())
    }

    @Test
    fun `observeAckSyncStatus delegates to pendingAckDao`() = runTest {
        val now = Instant.now()
        mockPendingAckDao.insertOrIgnore(
            com.sih26001.mobilealert.data.local.PendingAckEntity(
                alertId = "ALT-STATUS-1",
                acknowledgedAt = now,
                status = com.sih26001.mobilealert.data.local.AckSyncStatus.PENDING
            )
        )

        val status = repository.observeAckSyncStatus("ALT-STATUS-1").first()
        assertEquals(com.sih26001.mobilealert.data.local.AckSyncStatus.PENDING, status)
    }

    @Test
    fun `silenceAlert updates status but does not acknowledge and does not create pending ACK`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))
        repository.refreshAlerts()

        val silenceResult = repository.silenceAlert("ALT-1")
        assertTrue(silenceResult.isSuccess)

        // Silenced alerts should STILL appear in getActiveAlerts
        val active = repository.getActiveAlerts().first()
        assertEquals(1, active.size)
        assertEquals(AlertStatus.SILENCED, active[0].status)
        assertNull(active[0].acknowledgedAt)

        // It should NOT appear in history
        val history = repository.getAlertHistory().first()
        assertTrue(history.isEmpty())

        // Must NOT create pending ACK
        assertTrue(mockPendingAckDao.getPendingAcks().isEmpty())
    }

    @Test
    fun `silenceAlert on already ACKNOWLEDGED alert does not revert to SILENCED`() = runTest {
        mockApiService.mockResponse = listOf(createMockDto("ALT-1", "CRITICAL", "ACTIVE"))
        repository.refreshAlerts()

        repository.acknowledgeAlert("ALT-1")
        val ackAlert = repository.getAlertById("ALT-1").first()
        assertEquals(AlertStatus.ACKNOWLEDGED, ackAlert?.status)

        // Try silencing
        repository.silenceAlert("ALT-1")
        val postSilence = repository.getAlertById("ALT-1").first()
        assertEquals(AlertStatus.ACKNOWLEDGED, postSilence?.status)
    }

    @Test
    fun `CRITICAL, HIGH, and NORMAL alerts can all be acknowledged`() = runTest {
        mockApiService.mockResponse = listOf(
            createMockDto("ALT-CRIT", "CRITICAL", "ACTIVE"),
            createMockDto("ALT-HIGH", "HIGH", "ACTIVE"),
            createMockDto("ALT-NORM", "NORMAL", "ACTIVE")
        )
        repository.refreshAlerts()

        assertTrue(repository.acknowledgeAlert("ALT-CRIT").isSuccess)
        assertTrue(repository.acknowledgeAlert("ALT-HIGH").isSuccess)
        assertTrue(repository.acknowledgeAlert("ALT-NORM").isSuccess)

        val history = repository.getAlertHistory().first()
        assertEquals(3, history.size)
    }

    @Test
    fun `expired alert preserves EXPIRED status while recording acknowledgedAt`() = runTest {
        val expiredDto = AlertDto(
            alert_id = "ALT-EXP-ACK",
            event_type = "LANDSLIDE_RISK",
            severity = "HIGH",
            risk_score = 75.0,
            location = LocationDto("Test", 10.0, 20.0),
            issued_at = "2020-01-01T00:00:00Z",
            expires_at = "2020-01-01T01:00:00Z",
            top_drivers = listOf("Rain"),
            recommended_action = "Stay alert",
            affected_assets = emptyList(),
            source = "Test",
            data_quality = "GOOD",
            requires_ack = false,
            status = "ACTIVE"
        )
        mockApiService.mockResponse = listOf(expiredDto)
        repository.refreshAlert("ALT-EXP-ACK")

        val alertBefore = repository.getAlertById("ALT-EXP-ACK").first()
        assertEquals(AlertStatus.EXPIRED, alertBefore?.status)

        val ackResult = repository.acknowledgeAlert("ALT-EXP-ACK")
        assertTrue(ackResult.isSuccess)

        val alertAfter = repository.getAlertById("ALT-EXP-ACK").first()
        // Status must remain EXPIRED, not reverted to ACTIVE or ACKNOWLEDGED
        assertEquals(AlertStatus.EXPIRED, alertAfter?.status)
        assertNotNull(alertAfter?.acknowledgedAt)
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

    override fun getAlertById(id: String): AlertEntity? {
        return entities.value[id]
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

class FakePendingAckDao : com.sih26001.mobilealert.data.local.PendingAckDao {
    private val acks = MutableStateFlow<Map<String, com.sih26001.mobilealert.data.local.PendingAckEntity>>(emptyMap())

    override fun observePendingAcks(): Flow<List<com.sih26001.mobilealert.data.local.PendingAckEntity>> {
        return acks.map { it.values.sortedBy { ack -> ack.acknowledgedAt } }
    }

    override fun getPendingAcks(): List<com.sih26001.mobilealert.data.local.PendingAckEntity> {
        return acks.value.values.sortedBy { it.acknowledgedAt }
    }

    override fun getEligiblePendingAcks(): List<com.sih26001.mobilealert.data.local.PendingAckEntity> {
        return acks.value.values.filter { it.status != com.sih26001.mobilealert.data.local.AckSyncStatus.COMPLETED }.sortedBy { it.acknowledgedAt }
    }

    override fun getPendingAckById(alertId: String): com.sih26001.mobilealert.data.local.PendingAckEntity? {
        return acks.value[alertId]
    }

    override fun observeAckSyncStatus(alertId: String): Flow<com.sih26001.mobilealert.data.local.AckSyncStatus?> {
        return acks.map { it[alertId]?.status }
    }

    override fun insertOrIgnore(entity: com.sih26001.mobilealert.data.local.PendingAckEntity): Long {
        val current = acks.value.toMutableMap()
        if (current.containsKey(entity.alertId)) {
            return -1L
        }
        current[entity.alertId] = entity
        acks.value = current
        return 1L
    }

    override fun update(entity: com.sih26001.mobilealert.data.local.PendingAckEntity) {
        val current = acks.value.toMutableMap()
        current[entity.alertId] = entity
        acks.value = current
    }

    override fun delete(alertId: String) {
        val current = acks.value.toMutableMap()
        current.remove(alertId)
        acks.value = current
    }
}
