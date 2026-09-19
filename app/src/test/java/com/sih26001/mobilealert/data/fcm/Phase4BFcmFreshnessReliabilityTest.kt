package com.sih26001.mobilealert.data.fcm

import com.sih26001.mobilealert.core.alarm.AlarmController
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.io.IOException
import java.time.Instant

/**
 * Phase 4B — FCM Delivery -> Alert Freshness & Reliability Tests.
 *
 * Covers:
 * - Test A: Existing valid trigger (FCM -> REST -> validation -> mapping -> Room)
 * - Test B: Duplicate trigger (Idempotency and Room uniqueness)
 * - Test C: Existing alert refresh (Server updates replace stale local state without duplicate rows)
 * - Test D: Temporary network failure (Bounded retry recovers and persists alert)
 * - Test E: Permanent network failure (Fails safely after bounded retries, no crash, no fabrication)
 * - Test F: Unknown alert ID (Safe failure, no fabricated alert, no Room insert)
 * - Test G: Invalid authoritative response (Validation failure aborts immediately, no Room insert)
 * - Test H: FCM payload remains strict (Extraneous payloads without alert_id rejected)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Phase4BFcmFreshnessReliabilityTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockNotificationManager: AlertNotificationManager = mock()
    private val mockAlarmController: AlarmController = mock()
    private val mockPendingAckDao: PendingAckDao = mock()

    private lateinit var fakeApiService: FakeReliabilityApiService
    private lateinit var fakeAlertDao: FakeReliabilityAlertDao

    private lateinit var repository: AlertRepositoryImpl
    private lateinit var fcmTriggerHandler: FcmAlertTriggerHandlerImpl
    private lateinit var fcmProcessor: FcmMessageProcessor

    private val pilotAlertId = "ALT-TAW-1789811514-4E51"

    @Before
    fun setUp() {
        fakeApiService = FakeReliabilityApiService()
        fakeAlertDao = FakeReliabilityAlertDao()

        repository = AlertRepositoryImpl(
            apiService = fakeApiService,
            alertDao = fakeAlertDao,
            pendingAckDao = mockPendingAckDao,
            ioDispatcher = testDispatcher
        )

        fcmTriggerHandler = FcmAlertTriggerHandlerImpl(
            alertRepository = repository,
            notificationManager = mockNotificationManager,
            alarmController = mockAlarmController,
            coroutineScope = testScope,
            maxRetries = 3,
            initialDelayMs = 100L,
            backoffMultiplier = 2.0
        )

        fcmProcessor = FcmMessageProcessor(maxCacheSize = 100)
    }

    // =========================================================================
    // Test A — Existing Valid Trigger
    // =========================================================================
    @Test
    fun `Test A - existing valid trigger executes full pipeline to Room`() = testScope.runTest {
        fakeApiService.alerts = listOf(createTawangDto(pilotAlertId, riskScore = 68.0))

        val result = fcmProcessor.processData(mapOf("alert_id" to pilotAlertId))
        assertTrue(result is FcmProcessingResult.ValidAlert)

        val alertId = (result as FcmProcessingResult.ValidAlert).alertId
        fcmTriggerHandler.handleAlertTrigger(alertId)
        advanceUntilIdle()

        val persisted = fakeAlertDao.getAlertById(pilotAlertId)
        assertNotNull("Authoritative alert must be persisted in Room", persisted)
        assertEquals(pilotAlertId, persisted?.alertId)
        assertEquals(AlertSeverity.HIGH, persisted?.severity)
        assertEquals(AlertStatus.ACTIVE, persisted?.status)
        assertEquals(68.0, persisted?.riskScore ?: 0.0, 0.01)
        assertEquals("Tawang", persisted?.locationName)
        assertEquals("LANDSLIDE_WARNING", persisted?.eventType)

        verify(mockNotificationManager).showAlertNotification(any())
        verify(mockAlarmController).startAlarm(any())
    }

    // =========================================================================
    // Test B — Duplicate Trigger
    // =========================================================================
    @Test
    fun `Test B - duplicate trigger preserves exactly one Room record`() = testScope.runTest {
        fakeApiService.alerts = listOf(createTawangDto(pilotAlertId, riskScore = 68.0))
        val payload = mapOf("alert_id" to pilotAlertId)

        // First delivery
        val result1 = fcmProcessor.processData(payload)
        assertTrue(result1 is FcmProcessingResult.ValidAlert)
        fcmTriggerHandler.handleAlertTrigger((result1 as FcmProcessingResult.ValidAlert).alertId)
        advanceUntilIdle()

        // Second delivery (duplicate)
        val result2 = fcmProcessor.processData(payload)
        assertTrue(result2 is FcmProcessingResult.DuplicateAlert)

        // Even if trigger handler is directly re-invoked with the same ID, Room enforces uniqueness
        fcmTriggerHandler.handleAlertTrigger(pilotAlertId)
        advanceUntilIdle()

        val allRecords = fakeAlertDao.getAllAlertsList()
        assertEquals("Room must contain exactly 1 row for the alert_id", 1, allRecords.size)
        assertEquals(pilotAlertId, allRecords.first().alertId)
    }

    // =========================================================================
    // Test C — Existing Alert Refresh
    // =========================================================================
    @Test
    fun `Test C - existing alert refresh updates existing Room record in place`() = testScope.runTest {
        // Pre-populate Room with an older version of the pilot alert (e.g. riskScore 50.0)
        val initialEntity = AlertEntity(
            alertId = pilotAlertId,
            eventType = "LANDSLIDE_WARNING",
            severity = AlertSeverity.NORMAL,
            riskScore = 50.0,
            locationName = "Tawang",
            latitude = 27.5861,
            longitude = 91.8694,
            issuedAt = Instant.now(),
            expiresAt = null,
            topDrivers = listOf("Initial Monitoring"),
            recommendedAction = "Monitor situation",
            affectedAssets = emptyList(),
            source = "risk_fusion",
            dataQuality = "FULL",
            requiresAck = true,
            status = AlertStatus.ACTIVE,
            receivedAt = Instant.now(),
            acknowledgedAt = null
        )
        fakeAlertDao.insertAlert(initialEntity)
        assertEquals(1, fakeAlertDao.getAllAlertsList().size)
        assertEquals(50.0, fakeAlertDao.getAlertById(pilotAlertId)?.riskScore ?: 0.0, 0.01)

        // Backend now has the escalated authoritative alert (severity HIGH, riskScore 68.0)
        fakeApiService.alerts = listOf(createTawangDto(pilotAlertId, severity = "HIGH", riskScore = 68.0))

        // Trigger FCM for the existing alert
        fcmTriggerHandler.handleAlertTrigger(pilotAlertId)
        advanceUntilIdle()

        // Verify Room updated in place without creating a second record
        val allRecords = fakeAlertDao.getAllAlertsList()
        assertEquals("Room record count must remain 1 after refresh", 1, allRecords.size)

        val updated = fakeAlertDao.getAlertById(pilotAlertId)
        assertNotNull(updated)
        assertEquals(AlertSeverity.HIGH, updated?.severity)
        assertEquals(68.0, updated?.riskScore ?: 0.0, 0.01)
    }

    // =========================================================================
    // Test D — Temporary Network Failure
    // =========================================================================
    @Test
    fun `Test D - temporary network failure recovers via bounded retry`() = testScope.runTest {
        fakeApiService.alerts = listOf(createTawangDto(pilotAlertId, riskScore = 68.0))
        // Fail only the first attempt
        fakeApiService.failAttemptsRemaining = 1

        fcmTriggerHandler.handleAlertTrigger(pilotAlertId)
        advanceUntilIdle()

        val persisted = fakeAlertDao.getAlertById(pilotAlertId)
        assertNotNull("Alert must be persisted after transient network failure recovery", persisted)
        assertEquals(pilotAlertId, persisted?.alertId)
        assertEquals(2, fakeApiService.callCount) // Attempt 1 failed, Attempt 2 succeeded
        verify(mockNotificationManager).showAlertNotification(any())
    }

    // =========================================================================
    // Test E — Permanent Network Failure
    // =========================================================================
    @Test
    fun `Test E - permanent network failure aborts bounded retries safely without crash or persistence`() = testScope.runTest {
        fakeApiService.failAttemptsRemaining = 100 // Always fail

        fcmTriggerHandler.handleAlertTrigger(pilotAlertId)
        advanceUntilIdle()

        // Must attempt initial + 3 retries = 4 total attempts
        assertEquals(4, fakeApiService.callCount)

        val persisted = fakeAlertDao.getAlertById(pilotAlertId)
        assertNull("No alert should be persisted after permanent network failure", persisted)
        assertEquals(0, fakeAlertDao.getAllAlertsList().size)
        verify(mockNotificationManager, never()).showAlertNotification(any())
    }

    // =========================================================================
    // Test F — Unknown Alert ID
    // =========================================================================
    @Test
    fun `Test F - unknown alert ID fails safely without fabricated alert or Room insert`() = testScope.runTest {
        val unknownId = "ALT-UNKNOWN-999"
        // Backend only has pilot alert, not the unknown ID
        fakeApiService.alerts = listOf(createTawangDto(pilotAlertId, riskScore = 68.0))

        fcmTriggerHandler.handleAlertTrigger(unknownId)
        advanceUntilIdle()

        val persisted = fakeAlertDao.getAlertById(unknownId)
        assertNull("Unknown alert must not be persisted", persisted)
        assertEquals(0, fakeAlertDao.getAllAlertsList().size)
        verify(mockNotificationManager, never()).showAlertNotification(any())
    }

    // =========================================================================
    // Test G — Invalid Authoritative Response
    // =========================================================================
    @Test
    fun `Test G - invalid authoritative response rejected without retry or persistence`() = testScope.runTest {
        // Return malformed DTO with unknown severity
        val malformedDto = createTawangDto(pilotAlertId).copy(severity = "INVALID_SEVERITY")
        fakeApiService.alerts = listOf(malformedDto)

        fcmTriggerHandler.handleAlertTrigger(pilotAlertId)
        advanceUntilIdle()

        // Validation failures must abort immediately on attempt 1 without wasting retries
        assertEquals(1, fakeApiService.callCount)

        val persisted = fakeAlertDao.getAlertById(pilotAlertId)
        assertNull("Malformed DTO must never be persisted", persisted)
        assertEquals(0, fakeAlertDao.getAllAlertsList().size)
        verify(mockNotificationManager, never()).showAlertNotification(any())
    }

    // =========================================================================
    // Test H — FCM Payload Remains Strict
    // =========================================================================
    @Test
    fun `Test H - non-contract payloads without valid alert_id are rejected`() = testScope.runTest {
        val nonContractPayload = mapOf(
            "severity" to "CRITICAL",
            "risk_score" to "95.0",
            "location" to "Tawang",
            "action" to "EVACUATE"
        )

        val result = fcmProcessor.processData(nonContractPayload)
        assertTrue("Payload missing alert_id key must be rejected", result is FcmProcessingResult.MissingAlertId)

        advanceUntilIdle()
        assertEquals(0, fakeApiService.callCount)
        assertEquals(0, fakeAlertDao.getAllAlertsList().size)
    }

    private fun createTawangDto(
        alertId: String,
        severity: String = "HIGH",
        riskScore: Double = 68.0
    ): AlertDto {
        return AlertDto(
            alert_id = alertId,
            event_type = "LANDSLIDE_WARNING",
            severity = severity,
            risk_score = riskScore,
            location = LocationDto("Tawang", 27.5861, 91.8694),
            issued_at = "2026-09-19T09:51:54.958713+00:00",
            expires_at = null,
            top_drivers = listOf("Kinematic Strain", "Terrain Susceptibility"),
            recommended_action = "HEIGHTENED WARNING: Restrict arterial traffic.",
            affected_assets = emptyList(),
            source = "risk_fusion",
            data_quality = "FULL",
            requires_ack = true,
            status = "ACTIVE"
        )
    }

    private class FakeReliabilityApiService : AlertApiService {
        var alerts: List<AlertDto> = emptyList()
        var failAttemptsRemaining: Int = 0
        var callCount: Int = 0

        override suspend fun getAlerts(): List<AlertDto> {
            callCount++
            if (failAttemptsRemaining > 0) {
                failAttemptsRemaining--
                throw IOException("Simulated network timeout/disconnect")
            }
            return alerts
        }
    }

    private class FakeReliabilityAlertDao : AlertDao {
        private val map = MutableStateFlow<Map<String, AlertEntity>>(emptyMap())

        fun getAllAlertsList(): List<AlertEntity> = map.value.values.toList()

        override fun observeAllAlerts(): Flow<List<AlertEntity>> {
            return map.map { it.values.toList() }
        }

        override fun observeActiveAlerts(activeStatuses: List<String>): Flow<List<AlertEntity>> {
            return map.map { currentMap ->
                currentMap.values.filter { it.status.name in activeStatuses }
            }
        }

        override fun observeAlertById(id: String): Flow<AlertEntity?> {
            return map.map { it[id] }
        }

        override fun getAlertById(id: String): AlertEntity? {
            return map.value[id]
        }

        override fun insertAlerts(alerts: List<AlertEntity>) {
            val updated = map.value.toMutableMap()
            alerts.forEach { updated[it.alertId] = it }
            map.value = updated
        }

        override fun insertAlert(alert: AlertEntity) {
            val updated = map.value.toMutableMap()
            updated[alert.alertId] = alert
            map.value = updated
        }

        override fun updateStatus(id: String, status: AlertStatus) {
            val current = map.value[id] ?: return
            insertAlert(current.copy(status = status))
        }

        override fun updateAcknowledgedAt(id: String, timestamp: Instant) {
            val current = map.value[id] ?: return
            insertAlert(current.copy(acknowledgedAt = timestamp, status = AlertStatus.ACKNOWLEDGED))
        }

        override fun deleteAllAlerts() {
            map.value = emptyMap()
        }
    }
}
