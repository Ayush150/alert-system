package com.sih26001.mobilealert

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.data.mock.MockAlertData
import com.sih26001.mobilealert.di.DependencyContainer
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
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var notificationManager: AlertNotificationManager
    private lateinit var alarmController: AlarmController
    private lateinit var mainViewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        notificationManager = mock(AlertNotificationManager::class.java)
        alarmController = mock(AlarmController::class.java)
        
        DependencyContainer.mockAlertRepository.clearAllAlerts()
        
        mainViewModel = MainViewModel(notificationManager, alarmController)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `new critical alert triggers notification and alarm`() = runTest {
        val alert = MockAlertData.criticalAlert
        
        DependencyContainer.mockAlertRepository.triggerTestAlert(alert)
        advanceUntilIdle()

        verify(notificationManager).showAlertNotification(alert)
        verify(alarmController).startAlarm(alert)
    }

    @Test
    fun `silencing an alert stops alarm but not notification`() = runTest {
        val alert = MockAlertData.criticalAlert
        
        DependencyContainer.mockAlertRepository.triggerTestAlert(alert)
        advanceUntilIdle()
        
        // Silence it
        DependencyContainer.mockAlertRepository.silenceAlert(alert.alertId)
        advanceUntilIdle()

        // It was triggered
        verify(notificationManager).showAlertNotification(alert)
        verify(alarmController).startAlarm(alert)
        
        // It was silenced
        verify(alarmController).silenceAlarm(alert.alertId)
    }

    @Test
    fun `acknowledging an alert stops alarm and notification`() = runTest {
        val alert = MockAlertData.criticalAlert
        
        DependencyContainer.mockAlertRepository.triggerTestAlert(alert)
        advanceUntilIdle()
        
        // Acknowledge it
        DependencyContainer.mockAlertRepository.acknowledgeAlert(alert.alertId)
        advanceUntilIdle()

        // It was removed from active alerts flow so it cancels notification
        verify(notificationManager).cancelAlertNotification(alert.alertId)
        verify(alarmController).silenceAlarm(alert.alertId)
    }
}
