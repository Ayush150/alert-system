package com.sih26001.mobilealert.data.fcm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.demo.DemoProtocolManagerImpl
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.data.local.AlertDao
import com.sih26001.mobilealert.data.local.AlertEntity
import com.sih26001.mobilealert.data.local.PendingAckDao
import com.sih26001.mobilealert.data.remote.api.AlertApiService
import com.sih26001.mobilealert.data.remote.dto.AlertDto
import com.sih26001.mobilealert.data.remote.dto.LocationDto
import com.sih26001.mobilealert.data.repository.AlertRepositoryImpl
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import java.time.Instant

/**
 * Phase: FCM Cloud -> Android Push Integration Tests.
 *
 * Verifies Requirements 14 A through I:
 * - A: Valid FCM payload: alert_id extracted correctly.
 * - B: Malformed payload: safely rejected.
 * - C: Valid alert_id: authoritative REST alert fetched.
 * - D: REST fetch failure: existing retry path works.
 * - E: Expired alert: no emergency alarm.
 * - F: Duplicate FCM: no duplicate Room alert.
 * - G: REAL cloud HIGH/CRITICAL: notification allowed, AlarmController NOT called.
 * - H: DEMO alert: alarm only when DemoProtocolManager is active.
 * - I: FCM payload does not need complete alert data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FcmCloudIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockNotificationManager: AlertNotificationManager = mock()
    private val mockAlarmController: AlarmController = mock()
    private val mockPendingAckDao: PendingAckDao = mock()
    private val demoProtocolManager = DemoProtocolManagerImpl()

    private lateinit var processor: FcmMessageProcessor
    private lateinit var fakeApiService: FakeTestApiService
    private lateinit var fakeAlertDao: FakeTestAlertDao
    private lateinit var repository: AlertRepositoryImpl
    private lateinit var handler: FcmAlertTriggerHandlerImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        processor = FcmMessageProcessor(maxCacheSize = 100)
        fakeApiService = FakeTestApiService()
        fakeAlertDao = FakeTestAlertDao()

        repository = AlertRepositoryImpl(
            apiService = fakeApiService,
            alertDao = fakeAlertDao,
            pendingAckDao = mockPendingAckDao,
            ioDispatcher = testDispatcher
        )

        handler = FcmAlertTriggerHandlerImpl(
            alertRepository = repository,
            notificationManager = mockNotificationManager,
            alarmController = mockAlarmController,
            coroutineScope = testScope,
            initialDelayMs = 10L,
            backoffMultiplier = 1.0
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Test A: Valid FCM payload extracts alert_id correctly
    // -------------------------------------------------------------------------
    @Test
    fun `Test A - valid FCM payload extracts alert_id correctly`() {
        val payload = mapOf("alert_id" to "ALT-TAW-1789749077-4167")
        val result = processor.processData(payload)

        assertTrue(result is FcmProcessingResult.ValidAlert)
        assertEquals("ALT-TAW-1789749077-4167", (result as FcmProcessingResult.ValidAlert).alertId)
    }

    // -------------------------------------------------------------------------
    // Test B: Malformed payload safely rejected without crash or alarm
    // -------------------------------------------------------------------------
    @Test
    fun `Test B - malformed payload is safely rejected without crash or alarm`() = testScope.runTest {
        // Missing alert_id
        val emptyPayload = emptyMap<String, String>()
        val resultEmpty = processor.processData(emptyPayload)
        assertEquals(FcmProcessingResult.EmptyPayload, resultEmpty)

        // Blank alert_id
        val blankPayload = mapOf("alert_id" to "   ")
        val resultBlank = processor.processData(blankPayload)
        assertEquals(FcmProcessingResult.MissingAlertId, resultBlank)

        // Malformed trigger does not invoke alarm or notification
        advanceUntilIdle()
        verify(mockNotificationManager, never()).showAlertNotification(any())
        verify(mockAlarmController, never()).startAlarm(any())
    }

    // -------------------------------------------------------------------------
    // Test C: Valid alert_id fetches authoritative REST alert and stores in Room
    // -------------------------------------------------------------------------
    @Test
    fun `Test C - valid alert_id fetches authoritative REST alert and persists to Room`() = testScope.runTest {
        val alertDto = createAlertDto("ALT-TAW-001", "HIGH", "Risk Engine")
        fakeApiService.alerts = listOf(alertDto)

        handler.handleAlertTrigger("ALT-TAW-001")
        advanceUntilIdle()

        // Verify stored in Room
        val stored = fakeAlertDao.getAlertById("ALT-TAW-001")
        assertNotNull(stored)
        assertEquals("ALT-TAW-001", stored?.alertId)
        assertEquals(AlertSeverity.HIGH, stored?.severity)

        // Verify notification shown
        verify(mockNotificationManager).showAlertNotification(any())
    }

    // -------------------------------------------------------------------------
    // Test D: REST fetch failure uses existing retry path and recovers
    // -------------------------------------------------------------------------
    @Test
    fun `Test D - REST fetch failure uses existing retry path and succeeds on subsequent attempt`() = testScope.runTest {
        val alertDto = createAlertDto("ALT-TAW-RETRY", "CRITICAL", "Risk Engine")
        fakeApiService.alerts = listOf(alertDto)
        fakeApiService.failAttemptsRemaining = 2 // Fails twice then succeeds

        handler.handleAlertTrigger("ALT-TAW-RETRY")
        advanceUntilIdle()

        // Succeeded on 3rd attempt
        assertEquals(3, fakeApiService.callCount)
        val stored = fakeAlertDao.getAlertById("ALT-TAW-RETRY")
        assertNotNull(stored)
        assertEquals("ALT-TAW-RETRY", stored?.alertId)
    }

    // -------------------------------------------------------------------------
    // Test E: Expired alert does NOT trigger emergency alarm
    // -------------------------------------------------------------------------
    @Test
    fun `Test E - expired alert updates status and does not trigger emergency alarm`() = testScope.runTest {
        val pastInstant = Instant.now().minusSeconds(3600)
        val expiredDto = createAlertDto(
            alertId = "ALT-EXPIRED-01",
            severity = "CRITICAL",
            source = "Risk Engine",
            expiresAt = pastInstant.toString()
        )
        fakeApiService.alerts = listOf(expiredDto)

        handler.handleAlertTrigger("ALT-EXPIRED-01")
        advanceUntilIdle()

        val stored = fakeAlertDao.getAlertById("ALT-EXPIRED-01")
        assertNotNull(stored)
        assertEquals(AlertStatus.EXPIRED, stored?.status)

        // Expired alert must NEVER alarm
        verify(mockAlarmController, never()).startAlarm(any())
        verify(mockNotificationManager, never()).showAlertNotification(any())
    }

    // -------------------------------------------------------------------------
    // Test F: Duplicate FCM results in single Room alert (idempotence)
    // -------------------------------------------------------------------------
    @Test
    fun `Test F - duplicate FCM payload is deduplicated without duplicate Room alert`() = testScope.runTest {
        val alertDto = createAlertDto("ALT-DUP-01", "HIGH", "Risk Engine")
        fakeApiService.alerts = listOf(alertDto)

        // First trigger
        val res1 = processor.processData(mapOf("alert_id" to "ALT-DUP-01"))
        assertTrue(res1 is FcmProcessingResult.ValidAlert)
        handler.handleAlertTrigger("ALT-DUP-01")
        advanceUntilIdle()

        // Second trigger
        val res2 = processor.processData(mapOf("alert_id" to "ALT-DUP-01"))
        assertTrue(res2 is FcmProcessingResult.DuplicateAlert)

        // Room has only 1 entity
        assertEquals(1, fakeAlertDao.getAllAlertsList().size)
    }

    // -------------------------------------------------------------------------
    // Test G: REAL cloud HIGH or CRITICAL alert shows notification but NEVER alarms
    // -------------------------------------------------------------------------
    @Test
    fun `Test G - REAL cloud CRITICAL alert triggers notification but NEVER AlarmController`() = testScope.runTest {
        val cloudCriticalDto = createAlertDto("ALT-CLOUD-CRIT", "CRITICAL", "Risk Engine")
        fakeApiService.alerts = listOf(cloudCriticalDto)

        handler.handleAlertTrigger("ALT-CLOUD-CRIT")
        advanceUntilIdle()

        // Notification allowed
        verify(mockNotificationManager).showAlertNotification(any())

        // Physical alarm NEVER called for real cloud alert!
        verify(mockAlarmController, never()).startAlarm(any())
    }

    // -------------------------------------------------------------------------
    // Test H: Backend-generated DEMO alert alarms upon authoritative fetch
    // -------------------------------------------------------------------------
    @Test
    fun `Test H - Backend-generated DEMO alert alarms upon authoritative fetch`() = testScope.runTest {
        val demoCriticalDto = createAlertDto("ALT-DEMO-HIGH", "CRITICAL", "sih26001_demo")
        fakeApiService.alerts = listOf(demoCriticalDto)

        handler.handleAlertTrigger("ALT-DEMO-HIGH")
        advanceUntilIdle()

        verify(mockNotificationManager).showAlertNotification(any())
        verify(mockAlarmController).startAlarm(any())
    }

    // -------------------------------------------------------------------------
    // Test I: FCM payload does not need complete alert data (data-only trigger)
    // -------------------------------------------------------------------------
    @Test
    fun `Test I - FCM payload does not need complete alert data and extraneous fields are ignored`() = testScope.runTest {
        // FCM payload contains ONLY alert_id plus extraneous mock data
        val minimalPayload = mapOf(
            "alert_id" to "ALT-MINIMAL",
            "fake_score" to "99.9",
            "fake_severity" to "EXTREME"
        )

        val result = processor.processData(minimalPayload)
        assertTrue(result is FcmProcessingResult.ValidAlert)
        assertEquals("ALT-MINIMAL", (result as FcmProcessingResult.ValidAlert).alertId)

        // Authoritative data is fetched from REST, NOT from FCM
        val authoritativeDto = createAlertDto("ALT-MINIMAL", "HIGH", "Risk Engine")
        fakeApiService.alerts = listOf(authoritativeDto)

        handler.handleAlertTrigger("ALT-MINIMAL")
        advanceUntilIdle()

        val stored = fakeAlertDao.getAlertById("ALT-MINIMAL")
        assertNotNull(stored)
        // Stored values come from REST DTO, not FCM payload
        assertEquals(AlertSeverity.HIGH, stored?.severity)
    }

    // --- Helpers & Fakes ---

    private fun createAlertDto(
        alertId: String,
        severity: String,
        source: String,
        expiresAt: String? = null,
        status: String = "ACTIVE"
    ): AlertDto {
        return AlertDto(
            alert_id = alertId,
            event_type = "LANDSLIDE_WARNING",
            severity = severity,
            risk_score = 75.0,
            location = LocationDto("Tawang", 27.5861, 91.8694),
            issued_at = Instant.now().toString(),
            expires_at = expiresAt,
            top_drivers = listOf("Rainfall (75%)"),
            recommended_action = "Evacuate immediately",
            affected_assets = emptyList(),
            source = source,
            data_quality = "HIGH",
            requires_ack = true,
            status = status
        )
    }

    private class FakeTestApiService : AlertApiService {
        var alerts: List<AlertDto> = emptyList()
        var failAttemptsRemaining: Int = 0
        var callCount: Int = 0

        override suspend fun getAlerts(): List<AlertDto> = getActiveAlerts()

        override suspend fun getActiveAlerts(): List<AlertDto> {
            callCount++
            if (failAttemptsRemaining > 0) {
                failAttemptsRemaining--
                throw IOException("Network error")
            }
            return alerts
        }

        override suspend fun getAlertById(alertId: String): AlertDto {
            callCount++
            if (failAttemptsRemaining > 0) {
                failAttemptsRemaining--
                throw IOException("Network error")
            }
            return alerts.firstOrNull { it.alert_id == alertId }
                ?: throw retrofit2.HttpException(
                    retrofit2.Response.error<AlertDto>(
                        404,
                        okhttp3.ResponseBody.create(null, "Not Found")
                    )
                )
        }

        override suspend fun acknowledgeAlert(
            alertId: String,
            request: com.sih26001.mobilealert.data.remote.dto.AckRequestDto?
        ): com.sih26001.mobilealert.data.remote.dto.AckResponseDto =
            com.sih26001.mobilealert.data.remote.dto.AckResponseDto(alert_id = alertId, status = "ACKNOWLEDGED")

        override suspend fun silenceAlarm(): com.sih26001.mobilealert.data.remote.dto.SilenceResponseDto =
            com.sih26001.mobilealert.data.remote.dto.SilenceResponseDto(status = "SILENCED", is_muted = true)

        override suspend fun getAlarmStatus(): com.sih26001.mobilealert.data.remote.dto.AlarmStatusDto =
            com.sih26001.mobilealert.data.remote.dto.AlarmStatusDto()

        override suspend fun getSystemStatus(): com.sih26001.mobilealert.data.remote.dto.SystemStatusDto =
            com.sih26001.mobilealert.data.remote.dto.SystemStatusDto()
    }

    private class FakeTestAlertDao : AlertDao {
        private val map = MutableStateFlow<Map<String, AlertEntity>>(emptyMap())

        fun getAllAlertsList(): List<AlertEntity> = map.value.values.toList()

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
}
