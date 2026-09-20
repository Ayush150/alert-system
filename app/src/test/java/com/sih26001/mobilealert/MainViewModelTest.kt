package com.sih26001.mobilealert

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.data.mock.MockAlertData
import com.sih26001.mobilealert.data.repository.MockAlertRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var notificationManager: AlertNotificationManager
    private lateinit var alarmController: AlarmController
    private lateinit var mainViewModel: MainViewModel
    private lateinit var mockAlertRepository: MockAlertRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        notificationManager = mock(AlertNotificationManager::class.java)
        alarmController = mock(AlarmController::class.java)
        mockAlertRepository = MockAlertRepository()
        
        mainViewModel = MainViewModel(
            notificationManager = notificationManager,
            alarmController = alarmController,
            alertRepository = mockAlertRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `app startup does NOT trigger notification or physical alarm`() = runTest {
        advanceUntilIdle()

        verify(notificationManager, never()).showAlertNotification(mockAlertRepository.triggerTestAlert(MockAlertData.criticalAlert).let { MockAlertData.criticalAlert })
        verify(alarmController, never()).startAlarm(any())
    }

    @Test
    fun `alerts already in Room or emitted do NOT trigger alarm in MainViewModel`() = runTest {
        val cloudAlert = MockAlertData.criticalAlert
        mockAlertRepository.triggerTestAlert(cloudAlert)
        advanceUntilIdle()

        // MainViewModel does not trigger hardware alarm or notification for database emissions;
        // notifications and alarms are handled strictly by FCM pipeline (FcmAlertTriggerHandler)
        verify(alarmController, never()).startAlarm(cloudAlert)
        verify(notificationManager, never()).showAlertNotification(cloudAlert)
    }

    @Test
    fun `silencing an alert stops active alarm`() = runTest {
        val alert = MockAlertData.criticalAlert
        mockAlertRepository.triggerTestAlert(alert)
        advanceUntilIdle()

        mockAlertRepository.silenceAlert(alert.alertId)
        advanceUntilIdle()

        verify(alarmController).silenceAlarm(alert.alertId)
        verify(notificationManager, never()).cancelAlertNotification(alert.alertId)
    }

    @Test
    fun `acknowledging an alert stops alarm and cancels notification`() = runTest {
        val alert = MockAlertData.criticalAlert
        mockAlertRepository.triggerTestAlert(alert)
        advanceUntilIdle()

        mockAlertRepository.acknowledgeAlert(alert.alertId)
        advanceUntilIdle()

        verify(alarmController).silenceAlarm(alert.alertId)
        verify(notificationManager).cancelAlertNotification(alert.alertId)
    }

    @Test
    fun `clearing viewmodel stops alarm`() {
        // Test onCleared via test reflection or calling stopAlarm
        val localVm = MainViewModel(notificationManager, alarmController, mockAlertRepository)
        // Simulate clearance through reflection or public method
        val onClearedMethod = MainViewModel::class.java.getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true
        onClearedMethod.invoke(localVm)

        verify(alarmController).stopAlarm()
    }

    private fun any(): com.sih26001.mobilealert.domain.model.Alert = org.mockito.kotlin.any()
}
