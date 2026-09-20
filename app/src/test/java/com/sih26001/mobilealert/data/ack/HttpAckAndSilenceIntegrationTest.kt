package com.sih26001.mobilealert.data.ack

import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.AlertDao
import com.sih26001.mobilealert.data.local.AlertEntity
import com.sih26001.mobilealert.data.local.PendingAckDao
import com.sih26001.mobilealert.data.local.PendingAckEntity
import com.sih26001.mobilealert.data.remote.api.AlertApiService
import com.sih26001.mobilealert.data.remote.dto.AckRequestDto
import com.sih26001.mobilealert.data.remote.dto.AckResponseDto
import com.sih26001.mobilealert.data.remote.dto.AlarmStatusDto
import com.sih26001.mobilealert.data.remote.dto.AlertDto
import com.sih26001.mobilealert.data.remote.dto.SilenceResponseDto
import com.sih26001.mobilealert.data.remote.dto.SystemStatusDto
import com.sih26001.mobilealert.data.repository.AlertRepositoryImpl
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.usecase.AcknowledgeAlertUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HttpAckAndSilenceIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeApi: FakeIntegrationApiService
    private lateinit var fakeAlertDao: FakeIntegrationAlertDao
    private lateinit var fakePendingAckDao: FakeIntegrationPendingAckDao
    private lateinit var fakeConnectivityMonitor: FakeIntegrationConnectivityMonitor

    private lateinit var httpAckDataSource: HttpAckSyncDataSource
    private lateinit var ackSyncEngine: AckSyncEngine
    private lateinit var repository: AlertRepositoryImpl
    private lateinit var acknowledgeAlertUseCase: AcknowledgeAlertUseCase

    @Before
    fun setUp() {
        fakeApi = FakeIntegrationApiService()
        fakeAlertDao = FakeIntegrationAlertDao()
        fakePendingAckDao = FakeIntegrationPendingAckDao()
        fakeConnectivityMonitor = FakeIntegrationConnectivityMonitor(online = true)

        httpAckDataSource = HttpAckSyncDataSource(fakeApi)

        ackSyncEngine = AckSyncEngine(
            pendingAckDao = fakePendingAckDao,
            ackSyncDataSource = httpAckDataSource,
            ackRetryPolicy = ExponentialBackoffAckRetryPolicy(baseDelayMs = 0L, multiplier = 1.0),
            ackRecoveryPolicy = DefaultAckRecoveryPolicy(),
            connectivityMonitor = fakeConnectivityMonitor,
            dispatcher = testDispatcher
        )

        repository = AlertRepositoryImpl(
            apiService = fakeApi,
            alertDao = fakeAlertDao,
            pendingAckDao = fakePendingAckDao,
            ioDispatcher = testDispatcher
        )

        acknowledgeAlertUseCase = AcknowledgeAlertUseCase(repository)
    }

    // 1. Successful remote ACK
    @Test
    fun `successful remote ACK synchronizes with API and marks COMPLETED in Room`() = testScope.runTest {
        val alertId = "ALT-101"
        insertTestAlert(alertId, AlertStatus.ACTIVE)

        // User acknowledges alert locally
        val ackResult = acknowledgeAlertUseCase(alertId)
        assertTrue("Local acknowledgement must succeed", ackResult.isSuccess)

        val localAlert = fakeAlertDao.getAlertById(alertId)
        assertEquals(AlertStatus.ACKNOWLEDGED, localAlert?.status)
        assertNotNull(localAlert?.acknowledgedAt)

        val pendingAck = fakePendingAckDao.getPendingAckById(alertId)
        assertNotNull(pendingAck)
        assertEquals(AckSyncStatus.PENDING, pendingAck?.status)

        // Engine reconciles pending ACKs via HttpAckSyncDataSource
        val processed = ackSyncEngine.reconcilePendingAcks()
        assertEquals(1, processed)
        assertEquals(1, fakeApi.ackCallCount)
        assertEquals(alertId, fakeApi.lastAckAlertId)

        val completedAck = fakePendingAckDao.getPendingAckById(alertId)
        assertNotNull(completedAck)
        assertEquals(AckSyncStatus.COMPLETED, completedAck?.status)
        assertNotNull("completedAt must be populated", completedAck?.completedAt)
    }

    // 2. Remote ACK failure
    @Test
    fun `remote ACK failure marks record FAILED and schedules retry without losing local ACK`() = testScope.runTest {
        val alertId = "ALT-102"
        insertTestAlert(alertId, AlertStatus.ACTIVE)
        fakeApi.ackShouldThrow = IOException("Simulated network timeout/disconnect")

        val ackResult = acknowledgeAlertUseCase(alertId)
        assertTrue("Local acknowledgement must succeed even if remote fails later", ackResult.isSuccess)

        val processed = ackSyncEngine.reconcilePendingAcks()
        assertEquals(1, processed)

        val failedAck = fakePendingAckDao.getPendingAckById(alertId)
        assertNotNull(failedAck)
        assertEquals(AckSyncStatus.FAILED, failedAck?.status)
        assertEquals(1, failedAck?.retryCount)
        assertNotNull("lastFailureAt must be populated", failedAck?.lastFailureAt)
        assertTrue(failedAck?.lastFailureMessage?.contains("Simulated network timeout") == true)

        // Local alert must remain ACKNOWLEDGED
        val localAlert = fakeAlertDao.getAlertById(alertId)
        assertEquals(AlertStatus.ACKNOWLEDGED, localAlert?.status)
    }

    // 3. Offline ACK remains queued
    @Test
    fun `offline ACK remains queued with status PENDING until network is available`() = testScope.runTest {
        val alertId = "ALT-103"
        insertTestAlert(alertId, AlertStatus.ACTIVE)
        fakeConnectivityMonitor.online = false

        val ackResult = acknowledgeAlertUseCase(alertId)
        assertTrue("Local acknowledgement must succeed offline", ackResult.isSuccess)

        // Reconcile attempt while offline
        val processed = ackSyncEngine.reconcilePendingAcks()
        assertEquals(0, processed) // Reconcile skipped due to offline

        val queuedAck = fakePendingAckDao.getPendingAckById(alertId)
        assertNotNull(queuedAck)
        assertEquals(AckSyncStatus.PENDING, queuedAck?.status)
        assertEquals(0, queuedAck?.retryCount)
        assertEquals(0, fakeApi.ackCallCount)
    }

    // 4. Retry after reconnect
    @Test
    fun `reconnecting to network synchronizes previously queued offline ACK`() = testScope.runTest {
        val alertId = "ALT-104"
        insertTestAlert(alertId, AlertStatus.ACTIVE)
        fakeConnectivityMonitor.online = false

        acknowledgeAlertUseCase(alertId)
        assertEquals(0, ackSyncEngine.reconcilePendingAcks())
        assertEquals(AckSyncStatus.PENDING, fakePendingAckDao.getPendingAckById(alertId)?.status)

        // Device reconnects to network
        fakeConnectivityMonitor.online = true
        val processed = ackSyncEngine.reconcilePendingAcks()

        assertEquals(1, processed)
        assertEquals(1, fakeApi.ackCallCount)

        val synchronizedAck = fakePendingAckDao.getPendingAckById(alertId)
        assertEquals(AckSyncStatus.COMPLETED, synchronizedAck?.status)
        assertNotNull(synchronizedAck?.completedAt)
    }

    // 5. Successful silence
    @Test
    fun `successful silence calls remote endpoint and updates local status to SILENCED`() = testScope.runTest {
        val alertId = "ALT-105"
        insertTestAlert(alertId, AlertStatus.ACTIVE)

        val result = repository.silenceAlert(alertId)
        assertTrue("Silence must report success", result.isSuccess)

        val alert = fakeAlertDao.getAlertById(alertId)
        assertEquals(AlertStatus.SILENCED, alert?.status)
        assertNull("Silence must never set acknowledgedAt", alert?.acknowledgedAt)
        assertEquals(1, fakeApi.silenceCallCount)

        // Verify silence does not queue into pending_acks
        assertNull(fakePendingAckDao.getPendingAckById(alertId))
    }

    // 6. Silence failure
    @Test
    fun `silence failure returns failure but preserves local SILENCED state`() = testScope.runTest {
        val alertId = "ALT-106"
        insertTestAlert(alertId, AlertStatus.ACTIVE)
        fakeApi.silenceShouldThrow = IOException("503 Gateway timeout")

        val result = repository.silenceAlert(alertId)
        assertTrue("Silence should return failure when remote API fails", result.isFailure)

        val alert = fakeAlertDao.getAlertById(alertId)
        assertEquals("Local status must still be SILENCED to preserve local audio muting", AlertStatus.SILENCED, alert?.status)
        assertNull("Silence failure must not acknowledge", alert?.acknowledgedAt)
        assertNull("No pending ACK should be created for silence", fakePendingAckDao.getPendingAckById(alertId))
    }

    // 7. ACK and SILENCE remain semantically separate
    @Test
    fun `ACK and SILENCE remain strictly semantically separate`() = testScope.runTest {
        val silenceId = "ALT-SILENCE-201"
        val ackId = "ALT-ACK-202"
        insertTestAlert(silenceId, AlertStatus.ACTIVE)
        insertTestAlert(ackId, AlertStatus.ACTIVE)

        // Action 1: Silence ALT-SILENCE-201
        repository.silenceAlert(silenceId)

        val silencedAlert = fakeAlertDao.getAlertById(silenceId)
        assertEquals(AlertStatus.SILENCED, silencedAlert?.status)
        assertNull(silencedAlert?.acknowledgedAt)
        assertNull(fakePendingAckDao.getPendingAckById(silenceId))
        assertEquals(1, fakeApi.silenceCallCount)
        assertEquals(0, fakeApi.ackCallCount)

        // Action 2: Acknowledge ALT-ACK-202
        acknowledgeAlertUseCase(ackId)

        val acknowledgedAlert = fakeAlertDao.getAlertById(ackId)
        assertEquals(AlertStatus.ACKNOWLEDGED, acknowledgedAlert?.status)
        assertNotNull(acknowledgedAlert?.acknowledgedAt)
        assertNotNull(fakePendingAckDao.getPendingAckById(ackId))

        // Action 3: Reconcile ACKs
        ackSyncEngine.reconcilePendingAcks()
        assertEquals(1, fakeApi.ackCallCount)
        assertEquals(ackId, fakeApi.lastAckAlertId)

        // Action 4: Later, operator can also acknowledge the previously silenced alert
        acknowledgeAlertUseCase(silenceId)
        val laterAckedAlert = fakeAlertDao.getAlertById(silenceId)
        assertEquals(AlertStatus.ACKNOWLEDGED, laterAckedAlert?.status)
        assertNotNull(laterAckedAlert?.acknowledgedAt)
        assertNotNull(fakePendingAckDao.getPendingAckById(silenceId))

        ackSyncEngine.reconcilePendingAcks()
        assertEquals(2, fakeApi.ackCallCount)
        assertEquals(silenceId, fakeApi.lastAckAlertId)
    }

    private fun insertTestAlert(id: String, status: AlertStatus) {
        fakeAlertDao.insertAlert(
            AlertEntity(
                alertId = id,
                eventType = "LANDSLIDE_WARNING",
                severity = AlertSeverity.HIGH,
                riskScore = 65.0,
                locationName = "Tawang",
                latitude = 27.58,
                longitude = 91.86,
                issuedAt = Instant.now(),
                expiresAt = null,
                topDrivers = listOf("Rainfall"),
                recommendedAction = "Caution",
                affectedAssets = emptyList(),
                source = "risk_fusion",
                dataQuality = "FULL",
                requiresAck = true,
                status = status,
                receivedAt = Instant.now(),
                acknowledgedAt = null
            )
        )
    }

    // --- Test Doubles ---

    private class FakeIntegrationApiService : AlertApiService {
        var ackCallCount = 0
        var silenceCallCount = 0
        var lastAckAlertId: String? = null
        var ackShouldThrow: Exception? = null
        var silenceShouldThrow: Exception? = null

        override suspend fun getAlerts(): List<AlertDto> = emptyList()
        override suspend fun getActiveAlerts(): List<AlertDto> = emptyList()
        override suspend fun getAlertById(alertId: String): AlertDto = throw NoSuchElementException()

        override suspend fun acknowledgeAlert(alertId: String, request: AckRequestDto?): AckResponseDto {
            ackCallCount++
            lastAckAlertId = alertId
            ackShouldThrow?.let { throw it }
            return AckResponseDto(
                alert_id = alertId,
                status = "ACKNOWLEDGED",
                timestamp = Instant.now().toString(),
                mqtt_published = true
            )
        }

        override suspend fun silenceAlarm(): SilenceResponseDto {
            silenceCallCount++
            silenceShouldThrow?.let { throw it }
            return SilenceResponseDto(
                status = "SILENCED",
                is_muted = true,
                alarm_state = "WARNING",
                active_alert_id = null
            )
        }

        override suspend fun getAlarmStatus(): AlarmStatusDto = AlarmStatusDto()
        override suspend fun getSystemStatus(): SystemStatusDto = SystemStatusDto()
    }

    private class FakeIntegrationConnectivityMonitor(var online: Boolean) : NetworkConnectivityMonitor {
        override fun isOnline(): Boolean = online
    }

    private class FakeIntegrationAlertDao : AlertDao {
        private val map = MutableStateFlow<Map<String, AlertEntity>>(emptyMap())

        override fun observeAllAlerts(): Flow<List<AlertEntity>> = map.map { it.values.toList() }
        override fun observeActiveAlerts(activeStatuses: List<String>): Flow<List<AlertEntity>> =
            map.map { it.values.filter { e -> activeStatuses.contains(e.status.name) } }
        override fun observeAlertById(id: String): Flow<AlertEntity?> = map.map { it[id] }
        override fun getAlertById(id: String): AlertEntity? = map.value[id]

        override fun insertAlerts(alerts: List<AlertEntity>) {
            val cur = map.value.toMutableMap()
            alerts.forEach { cur[it.alertId] = it }
            map.value = cur
        }

        override fun insertAlert(alert: AlertEntity) {
            val cur = map.value.toMutableMap()
            cur[alert.alertId] = alert
            map.value = cur
        }

        override fun updateStatus(id: String, status: AlertStatus) {
            val cur = map.value.toMutableMap()
            cur[id]?.let {
                cur[id] = it.copy(status = status)
                map.value = cur
            }
        }

        override fun updateAcknowledgedAt(id: String, timestamp: Instant) {
            val cur = map.value.toMutableMap()
            cur[id]?.let {
                cur[id] = it.copy(acknowledgedAt = timestamp)
                map.value = cur
            }
        }

        override fun deleteAllAlerts() {
            map.value = emptyMap()
        }

        override fun deleteAlertsBySource(source: String) {
            map.value = map.value.filterValues { it.source != source }
        }

        override fun deleteAlertsByIds(alertIds: List<String>) {
            map.value = map.value.filterKeys { !alertIds.contains(it) }
        }
    }

    private class FakeIntegrationPendingAckDao : PendingAckDao {
        private val map = MutableStateFlow<Map<String, PendingAckEntity>>(emptyMap())

        override fun observePendingAcks(): Flow<List<PendingAckEntity>> = map.map { it.values.toList() }
        override fun getPendingAcks(): List<PendingAckEntity> = map.value.values.toList()
        override fun getEligiblePendingAcks(): List<PendingAckEntity> =
            map.value.values.filter { it.status != AckSyncStatus.COMPLETED }.sortedBy { it.acknowledgedAt }
        override fun getPendingAckById(alertId: String): PendingAckEntity? = map.value[alertId]
        override fun observeAckSyncStatus(alertId: String): Flow<AckSyncStatus?> = map.map { it[alertId]?.status }

        override fun insertOrIgnore(entity: PendingAckEntity): Long {
            val cur = map.value.toMutableMap()
            if (cur.containsKey(entity.alertId)) return -1L
            cur[entity.alertId] = entity
            map.value = cur
            return 1L
        }

        override fun update(entity: PendingAckEntity) {
            val cur = map.value.toMutableMap()
            cur[entity.alertId] = entity
            map.value = cur
        }

        override fun delete(alertId: String) {
            val cur = map.value.toMutableMap()
            cur.remove(alertId)
            map.value = cur
        }
    }
}
