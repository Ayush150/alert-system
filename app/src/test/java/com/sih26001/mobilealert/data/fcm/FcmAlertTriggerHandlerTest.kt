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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
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
    fun `handleAlertTrigger with CRITICAL alert triggers both notification and alarm`() = testScope.runTest {
        val criticalAlert = createAlert("ALT-CRIT", AlertSeverity.CRITICAL, AlertStatus.ACTIVE)
        whenever(mockAlertRepository.refreshAlert("ALT-CRIT")).thenReturn(Result.success(criticalAlert))

        handler.handleAlertTrigger("ALT-CRIT")
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert("ALT-CRIT")
        verify(mockNotificationManager).showAlertNotification(criticalAlert)
        verify(mockAlarmController).startAlarm(criticalAlert)
    }

    @Test
    fun `handleAlertTrigger with HIGH alert triggers both notification and alarm`() = testScope.runTest {
        val highAlert = createAlert("ALT-HIGH", AlertSeverity.HIGH, AlertStatus.ACTIVE)
        whenever(mockAlertRepository.refreshAlert("ALT-HIGH")).thenReturn(Result.success(highAlert))

        handler.handleAlertTrigger("ALT-HIGH")
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert("ALT-HIGH")
        verify(mockNotificationManager).showAlertNotification(highAlert)
        verify(mockAlarmController).startAlarm(highAlert)
    }

    @Test
    fun `handleAlertTrigger with NORMAL alert triggers notification but NOT alarm`() = testScope.runTest {
        val normalAlert = createAlert("ALT-NORM", AlertSeverity.NORMAL, AlertStatus.ACTIVE)
        whenever(mockAlertRepository.refreshAlert("ALT-NORM")).thenReturn(Result.success(normalAlert))

        handler.handleAlertTrigger("ALT-NORM")
        advanceUntilIdle()

        verify(mockAlertRepository).refreshAlert("ALT-NORM")
        verify(mockNotificationManager).showAlertNotification(normalAlert)
        verify(mockAlarmController, never()).startAlarm(any())
    }

    @Test
    fun `handleAlertTrigger with EXPIRED alert triggers neither notification nor alarm`() = testScope.runTest {
        val expiredAlert = createAlert("ALT-EXP", AlertSeverity.CRITICAL, AlertStatus.EXPIRED)
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

        verify(mockAlertRepository).refreshAlert("ALT-FAIL")
        verify(mockNotificationManager, never()).showAlertNotification(any())
        verify(mockAlarmController, never()).startAlarm(any())
    }

    private fun createAlert(id: String, severity: AlertSeverity, status: AlertStatus): Alert {
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
            source = "test",
            dataQuality = "GOOD",
            requiresAck = false,
            status = status
        )
    }
}
