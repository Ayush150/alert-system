package com.sih26001.mobilealert.data.fcm

import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.repository.AlertRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FcmAlertTriggerHandlerTest {

    private val mockAlertRepository: AlertRepository = mock()
    private val mockNotificationManager: AlertNotificationManager = mock()
    private val mockAlarmController: AlarmController = mock()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var handler: FcmAlertTriggerHandlerImpl

    @Before
    fun setUp() {
        handler = FcmAlertTriggerHandlerImpl(
            alertRepository = mockAlertRepository,
            notificationManager = mockNotificationManager,
            alarmController = mockAlarmController,
            coroutineScope = testScope
        )
    }

    @Test
    fun `handleAlertTrigger with ordinary cloud CRITICAL alert triggers notification but NOT alarm`() = testScope.runTest {
        val criticalAlert = createAlert("ALT-CRIT", AlertSeverity.CRITICAL, AlertStatus.ACTIVE, source = "risk_engine")
        whenever(mockAlertRepository.refreshAlert("ALT-CRIT")).thenReturn(Result.success(criticalAlert))

        handler.handleAlertTrigger("ALT-CRIT")
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert("ALT-CRIT")
        verify(mockNotificationManager).showAlertNotification(criticalAlert)
        verify(mockAlarmController, never()).startAlarm(any())
    }

    @Test
    fun `handleAlertTrigger with backend-generated demo CRITICAL alert triggers both notification and alarm`() = testScope.runTest {
        val criticalAlert = createAlert("ALT-DEMO-CRIT", AlertSeverity.CRITICAL, AlertStatus.ACTIVE, source = "sih26001_demo")
        whenever(mockAlertRepository.refreshAlert("ALT-DEMO-CRIT")).thenReturn(Result.success(criticalAlert))

        handler.handleAlertTrigger("ALT-DEMO-CRIT")
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert("ALT-DEMO-CRIT")
        verify(mockNotificationManager).showAlertNotification(criticalAlert)
        verify(mockAlarmController).startAlarm(criticalAlert)
    }

    @Test
    fun `handleAlertTrigger with backend-generated demo HIGH alert triggers both notification and alarm`() = testScope.runTest {
        val highAlert = createAlert("ALT-DEMO-HIGH", AlertSeverity.HIGH, AlertStatus.ACTIVE, source = "sih26001_demo")
        whenever(mockAlertRepository.refreshAlert("ALT-DEMO-HIGH")).thenReturn(Result.success(highAlert))

        handler.handleAlertTrigger("ALT-DEMO-HIGH")
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert("ALT-DEMO-HIGH")
        verify(mockNotificationManager).showAlertNotification(highAlert)
        verify(mockAlarmController).startAlarm(highAlert)
    }

    @Test
    fun `handleAlertTrigger with demo NORMAL alert triggers notification but NOT alarm`() = testScope.runTest {
        val normalDemoAlert = createAlert("ALT-DEMO-NORM", AlertSeverity.NORMAL, AlertStatus.ACTIVE, source = "sih26001_demo")
        whenever(mockAlertRepository.refreshAlert("ALT-DEMO-NORM")).thenReturn(Result.success(normalDemoAlert))

        handler.handleAlertTrigger("ALT-DEMO-NORM")
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert("ALT-DEMO-NORM")
        verify(mockNotificationManager).showAlertNotification(normalDemoAlert)
        verify(mockAlarmController, never()).startAlarm(any())
    }

    @Test
    fun `handleAlertTrigger with NORMAL cloud alert triggers notification but NOT alarm`() = testScope.runTest {
        val normalAlert = createAlert("ALT-NORM", AlertSeverity.NORMAL, AlertStatus.ACTIVE, source = "risk_engine")
        whenever(mockAlertRepository.refreshAlert("ALT-NORM")).thenReturn(Result.success(normalAlert))

        handler.handleAlertTrigger("ALT-NORM")
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert("ALT-NORM")
        verify(mockNotificationManager).showAlertNotification(normalAlert)
        verify(mockAlarmController, never()).startAlarm(any())
    }

    @Test
    fun `handleAlertTrigger with EXPIRED alert triggers neither notification nor alarm`() = testScope.runTest {
        val expiredAlert = createAlert("ALT-EXP", AlertSeverity.CRITICAL, AlertStatus.EXPIRED, source = "sih26001_demo")
        whenever(mockAlertRepository.refreshAlert("ALT-EXP")).thenReturn(Result.success(expiredAlert))

        handler.handleAlertTrigger("ALT-EXP")
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert("ALT-EXP")
        verify(mockNotificationManager, never()).showAlertNotification(any())
        verify(mockAlarmController, never()).startAlarm(any())
    }

    @Test
    fun `handleAlertTrigger when repository fails handles error safely without crash`() = testScope.runTest {
        whenever(mockAlertRepository.refreshAlert("ALT-FAIL"))
            .thenReturn(Result.failure(IOException("Server error")))

        handler.handleAlertTrigger("ALT-FAIL")
        advanceUntilIdle()

        verify(mockAlertRepository, atLeastOnce()).refreshAlert("ALT-FAIL")
        verify(mockNotificationManager, never()).showAlertNotification(any())
        verify(mockAlarmController, never()).startAlarm(any())
    }

    @Test
    fun `handleAlertTrigger when validation fails never calls showAlertNotification`() = testScope.runTest {
        whenever(mockAlertRepository.refreshAlert("ALT-INVALID"))
            .thenReturn(Result.failure(IllegalArgumentException("Validation failed: severity is missing")))

        handler.handleAlertTrigger("ALT-INVALID")
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert("ALT-INVALID")
        verify(mockNotificationManager, never()).showAlertNotification(any())
        verify(mockAlarmController, never()).startAlarm(any())
    }

    @Test
    fun `handleAlertTrigger when alert ID is unknown never calls showAlertNotification`() = testScope.runTest {
        whenever(mockAlertRepository.refreshAlert("ALT-UNKNOWN"))
            .thenReturn(Result.failure(NoSuchElementException("Alert ALT-UNKNOWN not found")))

        handler.handleAlertTrigger("ALT-UNKNOWN")
        advanceUntilIdle()

        verify(mockAlertRepository, atLeastOnce()).refreshAlert("ALT-UNKNOWN")
        verify(mockNotificationManager, never()).showAlertNotification(any())
        verify(mockAlarmController, never()).startAlarm(any())
    }

    private fun createAlert(id: String, severity: AlertSeverity, status: AlertStatus, source: String = "test"): Alert {
        return Alert(
            alertId = id,
            eventType = "LANDSLIDE_RISK",
            severity = severity,
            riskScore = 80.0,
            location = null,
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            topDrivers = null,
            recommendedAction = "Stay safe",
            affectedAssets = null,
            source = source,
            dataQuality = "GOOD",
            requiresAck = false,
            status = status
        )
    }
}
