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
 * Phase 3D FCM Failure-Path, Duplicate, and Malformed-Payload Hardening Tests.
 * Verifies that the FCM receiving layer safely rejects invalid inputs, deduplicates incoming triggers,
 * and handles authoritative REST fetch errors without crashing or corrupting Room persistence.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Phase3DFcmHardeningTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockNotificationManager: AlertNotificationManager = mock()
    private val mockAlarmController: AlarmController = mock()
    private val mockPendingAckDao: PendingAckDao = mock()

    private lateinit var fakeApiService: FakeTestApiService
    private lateinit var fakeAlertDao: FakeTestAlertDao

    private lateinit var repository: AlertRepositoryImpl
    private lateinit var fcmTriggerHandler: FcmAlertTriggerHandlerImpl
    private lateinit var fcmProcessor: FcmMessageProcessor

    @Before
    fun setUp() {
        fakeApiService = FakeTestApiService()
        fakeAlertDao = FakeTestAlertDao()

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
            coroutineScope = testScope
        )
        fcmProcessor = FcmMessageProcessor(maxCacheSize = 100)
    }

    // Test 1: missingAlertId_isRejected
    @Test
    fun `Test 1 - missingAlertId_isRejected`() = testScope.runTest {
        val payload = emptyMap<String, String>()
        val result = fcmProcessor.processData(payload)

        assertTrue("Empty payload should return EmptyPayload", result is FcmProcessingResult.EmptyPayload)
        advanceUntilIdle()
        assertEquals(0, fakeAlertDao.getAllAlertsList().size)
    }

    // Test 2: blankAlertId_isRejected
    @Test
    fun `Test 2 - blankAlertId_isRejected`() = testScope.runTest {
        val emptyResult = fcmProcessor.processData(mapOf("alert_id" to ""))
        assertTrue("Empty alert_id should return MissingAlertId", emptyResult is FcmProcessingResult.MissingAlertId)

        val whitespaceResult = fcmProcessor.processData(mapOf("alert_id" to "   "))
        assertTrue("Whitespace alert_id should return MissingAlertId", whitespaceResult is FcmProcessingResult.MissingAlertId)

        advanceUntilIdle()
        assertEquals(0, fakeAlertDao.getAllAlertsList().size)
    }

    // Test 3: unexpectedPayload_withoutAlertId_isRejected
    @Test
    fun `Test 3 - unexpectedPayload_withoutAlertId_isRejected`() = testScope.runTest {
        val payload = mapOf(
            "severity" to "HIGH",
            "location" to "Tawang",
            "risk_score" to "99.0",
            "foo" to "bar"
        )
        val result = fcmProcessor.processData(payload)

        assertTrue("Payload missing alert_id key must return MissingAlertId", result is FcmProcessingResult.MissingAlertId)
        advanceUntilIdle()
        assertEquals("Locally supplied severity/location must NOT be persisted", 0, fakeAlertDao.getAllAlertsList().size)
    }

    // Test 4: validAlertId_isAccepted
    @Test
    fun `Test 4 - validAlertId_isAccepted`() = testScope.runTest {
        val alertId = "ALT-TAW-1789811514-4E51"
        fakeApiService.alerts = listOf(createValidTawangDto(alertId))

        val result = fcmProcessor.processData(mapOf("alert_id" to alertId))
        assertTrue("Valid alert_id must return ValidAlert", result is FcmProcessingResult.ValidAlert)

        val validId = (result as FcmProcessingResult.ValidAlert).alertId
        assertEquals(alertId, validId)

        fcmTriggerHandler.handleAlertTrigger(validId)
        advanceUntilIdle()

        val persisted = fakeAlertDao.getAlertById(alertId)
        assertNotNull("Authoritative alert must be persisted in Room", persisted)
        assertEquals(AlertSeverity.HIGH, persisted?.severity)
        assertEquals("Tawang", persisted?.locationName)
        assertEquals(AlertStatus.ACTIVE, persisted?.status)
        assertEquals(68.0, persisted?.riskScore ?: 0.0, 0.01)
    }

    // Test 5: duplicateAlertDelivery_doesNotCreateDuplicateRoomRecord
    @Test
    fun `Test 5 - duplicateAlertDelivery_doesNotCreateDuplicateRoomRecord`() = testScope.runTest {
        val alertId = "ALT-TAW-1789811514-4E51"
        fakeApiService.alerts = listOf(createValidTawangDto(alertId))

        val payload = mapOf("alert_id" to alertId)
        val result1 = fcmProcessor.processData(payload)
        assertTrue(result1 is FcmProcessingResult.ValidAlert)
        fcmTriggerHandler.handleAlertTrigger((result1 as FcmProcessingResult.ValidAlert).alertId)
        advanceUntilIdle()

        // Second delivery of identical FCM payload
        val result2 = fcmProcessor.processData(payload)
        assertTrue("Second delivery must be identified as DuplicateAlert", result2 is FcmProcessingResult.DuplicateAlert)

        // Even if handler were invoked again, Room primary key uniqueness and replace policy prevents duplicates
        fcmTriggerHandler.handleAlertTrigger(alertId)
        advanceUntilIdle()

        val allAlerts = fakeAlertDao.getAllAlertsList()
        val matchingCount = allAlerts.count { it.alertId == alertId }
        assertEquals("Exactly 1 Room record must exist for the alert_id", 1, matchingCount)
    }

    // Test 6: unknownAlertId_doesNotCreateAlert
    @Test
    fun `Test 6 - unknownAlertId_doesNotCreateAlert`() = testScope.runTest {
        val unknownId = "ALT-NONEXISTENT-TEST"
        // Backend only knows Tawang alert, does not know ALT-NONEXISTENT-TEST
        fakeApiService.alerts = listOf(createValidTawangDto("ALT-TAW-1789811514-4E51"))

        val result = fcmProcessor.processData(mapOf("alert_id" to unknownId))
        assertTrue(result is FcmProcessingResult.ValidAlert)

        // Authoritative fetch attempted for unknown ID
        fcmTriggerHandler.handleAlertTrigger((result as FcmProcessingResult.ValidAlert).alertId)
        advanceUntilIdle()

        // Verify no record inserted in Room for the unknown ID
        val persisted = fakeAlertDao.getAlertById(unknownId)
        assertNull("Unknown alert_id must NOT produce a Room record", persisted)
        verify(mockNotificationManager, never()).showAlertNotification(any<Alert>())
    }

    // Test 7: authoritativeFetchFailure_doesNotCrashOrPersist
    @Test
    fun `Test 7 - authoritativeFetchFailure_doesNotCrashOrPersist`() = testScope.runTest {
        val alertId = "ALT-TAW-1789811514-4E51"
        fakeApiService.shouldThrowNetworkError = true

        val result = fcmProcessor.processData(mapOf("alert_id" to alertId))
        assertTrue(result is FcmProcessingResult.ValidAlert)

        // Must handle network failure safely without crashing
        fcmTriggerHandler.handleAlertTrigger((result as FcmProcessingResult.ValidAlert).alertId)
        advanceUntilIdle()

        val persisted = fakeAlertDao.getAlertById(alertId)
        assertNull("Network failure must NOT persist any alert", persisted)
        verify(mockNotificationManager, never()).showAlertNotification(any<Alert>())
    }

    // Test 8: invalidAuthoritativeDto_doesNotPersist
    @Test
    fun `Test 8 - invalidAuthoritativeDto_doesNotPersist`() = testScope.runTest {
        val alertId = "ALT-MALFORMED"
        // DTO with unrecognized severity that fails AlertValidator
        val malformedDto = createValidTawangDto(alertId).copy(severity = "INVALID_SEVERITY_UNKNOWN")
        fakeApiService.alerts = listOf(malformedDto)

        val result = fcmProcessor.processData(mapOf("alert_id" to alertId))
        assertTrue(result is FcmProcessingResult.ValidAlert)

        fcmTriggerHandler.handleAlertTrigger((result as FcmProcessingResult.ValidAlert).alertId)
        advanceUntilIdle()

        val persisted = fakeAlertDao.getAlertById(alertId)
        assertNull("Malformed DTO failing AlertValidator must NOT be persisted", persisted)
        verify(mockNotificationManager, never()).showAlertNotification(any<Alert>())
    }

    private fun createValidTawangDto(alertId: String): AlertDto {
        return AlertDto(
            alert_id = alertId,
            event_type = "LANDSLIDE_WARNING",
            severity = "HIGH",
            risk_score = 68.0,
            location = LocationDto("Tawang", 27.5861, 91.8694),
            issued_at = "2026-09-19T09:51:54.958713+00:00",
            expires_at = null,
            top_drivers = listOf("Rainfall", "Slope"),
            recommended_action = "HEIGHTENED WARNING: Notify local administration.",
            affected_assets = emptyList(),
            source = "risk_fusion",
            data_quality = "FULL",
            requires_ack = true,
            status = "ACTIVE"
        )
    }

    private class FakeTestApiService : AlertApiService {
        var alerts: List<AlertDto> = emptyList()
        var shouldThrowNetworkError: Boolean = false

        override suspend fun getAlerts(): List<AlertDto> {
            if (shouldThrowNetworkError) {
                throw IOException("Simulated network timeout connecting to Sixth Sense")
            }
            return alerts
        }
    }

    private class FakeTestAlertDao : AlertDao {
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
