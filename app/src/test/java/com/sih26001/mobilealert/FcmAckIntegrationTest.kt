package com.sih26001.mobilealert

import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.data.fcm.FcmAlertTriggerHandlerImpl
import com.sih26001.mobilealert.data.fcm.FcmMessageProcessor
import com.sih26001.mobilealert.data.fcm.FcmProcessingResult
import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.AlertDao
import com.sih26001.mobilealert.data.local.AlertEntity
import com.sih26001.mobilealert.data.local.PendingAckDao
import com.sih26001.mobilealert.data.local.PendingAckEntity
import com.sih26001.mobilealert.data.remote.api.AlertApiService
import com.sih26001.mobilealert.data.remote.dto.AlertDto
import com.sih26001.mobilealert.data.remote.dto.LocationDto
import com.sih26001.mobilealert.data.repository.AlertRepositoryImpl
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.usecase.AcknowledgeAlertUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FcmAckIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockNotificationManager: AlertNotificationManager = mock()
    private val mockAlarmController: AlarmController = mock()

    private lateinit var fakeApiService: FakeTestApiService
    private lateinit var fakeAlertDao: FakeTestAlertDao
    private lateinit var fakePendingAckDao: FakeTestPendingAckDao

    private lateinit var repository: AlertRepositoryImpl
    private lateinit var fcmTriggerHandler: FcmAlertTriggerHandlerImpl
    private lateinit var acknowledgeAlertUseCase: AcknowledgeAlertUseCase
    private lateinit var fcmProcessor: FcmMessageProcessor

    @Before
    fun setUp() {
        fakeApiService = FakeTestApiService()
        fakeAlertDao = FakeTestAlertDao()
        fakePendingAckDao = FakeTestPendingAckDao()

        repository = AlertRepositoryImpl(
            apiService = fakeApiService,
            alertDao = fakeAlertDao,
            pendingAckDao = fakePendingAckDao,
            ioDispatcher = testDispatcher
        )
        fcmTriggerHandler = FcmAlertTriggerHandlerImpl(
            alertRepository = repository,
            notificationManager = mockNotificationManager,
            alarmController = mockAlarmController,
            coroutineScope = testScope
        )
        acknowledgeAlertUseCase = AcknowledgeAlertUseCase(repository)
        fcmProcessor = FcmMessageProcessor()
    }

    @Test
    fun `complete flow - FCM trigger fetches and persists ACTIVE alert then ACK acknowledges and queues`() = testScope.runTest {
        val alertId = "ALT-2026-FCM-ACK"
        fakeApiService.alerts = listOf(
            AlertDto(
                alert_id = alertId,
                event_type = "LANDSLIDE_RISK",
                severity = "CRITICAL",
                risk_score = 90.0,
                location = LocationDto("Zone A", 30.0, 78.0),
                issued_at = "2026-09-08T18:30:00Z",
                expires_at = "2028-09-08T22:30:00Z",
                top_drivers = listOf("Rainfall"),
                recommended_action = "Evacuate",
                affected_assets = emptyList(),
                source = "SIH",
                data_quality = "HIGH",
                requires_ack = true,
                status = "ACTIVE"
            )
        )

        // 1. FCM message arrives
        val fcmPayload = mapOf("alert_id" to alertId)
        val processResult = fcmProcessor.processData(fcmPayload)
        assertTrue(processResult is FcmProcessingResult.ValidAlert)
        val validAlertId = (processResult as FcmProcessingResult.ValidAlert).alertId

        // 2. FcmAlertTriggerHandler processes trigger
        fcmTriggerHandler.handleAlertTrigger(validAlertId)
        advanceUntilIdle()

        // 3. Verify alert is persisted in Room with ACTIVE status (FCM does NOT acknowledge!)
        val persisted = repository.getAlertById(alertId).first()
        assertNotNull(persisted)
        assertEquals(AlertStatus.ACTIVE, persisted?.status)
        assertNull(persisted?.acknowledgedAt)
        // Verify no pending ACK yet
        assertTrue(fakePendingAckDao.getPendingAcks().isEmpty())

        // Verify alarm and notification were triggered
        verify(mockNotificationManager).showAlertNotification(persisted!!)
        verify(mockAlarmController).startAlarm(persisted)

        // 4. User presses ACKNOWLEDGE in UI -> invokes AcknowledgeAlertUseCase
        val ackResult = acknowledgeAlertUseCase(alertId)
        assertTrue(ackResult.isSuccess)

        // 5. Verify alert transitioned to ACKNOWLEDGED with acknowledgedAt
        val acknowledgedAlert = repository.getAlertById(alertId).first()
        assertNotNull(acknowledgedAlert)
        assertEquals(AlertStatus.ACKNOWLEDGED, acknowledgedAlert?.status)
        assertNotNull(acknowledgedAlert?.acknowledgedAt)

        // 6. Verify PendingAckEntity created in Room queue for backend synchronization
        val pendingAcks = fakePendingAckDao.getPendingAcks()
        assertEquals(1, pendingAcks.size)
        assertEquals(alertId, pendingAcks[0].alertId)
        assertEquals(AckSyncStatus.PENDING, pendingAcks[0].status)

        // 7. Duplicate ACK call is idempotent
        val duplicateAckResult = acknowledgeAlertUseCase(alertId)
        assertTrue(duplicateAckResult.isSuccess)
        assertEquals(1, fakePendingAckDao.getPendingAcks().size)
    }

    @Test
    fun `FCM deduplication prevents duplicate triggers`() {
        val fcmPayload = mapOf("alert_id" to "ALT-DEDUP")
        val result1 = fcmProcessor.processData(fcmPayload)
        val result2 = fcmProcessor.processData(fcmPayload)

        assertTrue(result1 is FcmProcessingResult.ValidAlert)
        assertTrue(result2 is FcmProcessingResult.DuplicateAlert)
    }

    private class FakeTestApiService : AlertApiService {
        var alerts: List<AlertDto> = emptyList()
        override suspend fun getAlerts(): List<AlertDto> = alerts
    }

    private class FakeTestAlertDao : AlertDao {
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
    }

    private class FakeTestPendingAckDao : PendingAckDao {
        private val map = MutableStateFlow<Map<String, PendingAckEntity>>(emptyMap())

        override fun observePendingAcks(): Flow<List<PendingAckEntity>> = map.map { it.values.toList() }
        override fun getPendingAcks(): List<PendingAckEntity> = map.value.values.toList()
        override fun getEligiblePendingAcks(): List<PendingAckEntity> = map.value.values.filter { it.status != AckSyncStatus.COMPLETED }
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
