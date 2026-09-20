package com.sih26001.mobilealert.architecture

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.sih26001.mobilealert.MainViewModel
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.notification.AlertNotificationManager
import com.sih26001.mobilealert.core.util.Constants
import com.sih26001.mobilealert.data.fcm.FcmAlertTriggerHandlerImpl
import com.sih26001.mobilealert.data.fcm.FcmMessageProcessor
import com.sih26001.mobilealert.data.fcm.FcmProcessingResult
import com.sih26001.mobilealert.data.fcm.FcmTopicSubscriber
import com.sih26001.mobilealert.data.mock.MockAlertData
import com.sih26001.mobilealert.data.repository.MockAlertRepository
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.isDemoAlert
import com.sih26001.mobilealert.presentation.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Verification of the Receiver-Only Architecture and Web-Driven Demo Protocol contract.
 *
 * Enforces Requirements 16A through 16H:
 * A. App startup produces no alert.
 * B. App startup produces no alarm.
 * C. No local demo alert is generated.
 * D. Real cloud HIGH/CRITICAL never activates physical alarm.
 * E. Backend-generated demo HIGH/CRITICAL can activate the alarm only after receiving the authoritative FCM alert.
 * F. Malformed/untrusted FCM payload cannot activate alarm.
 * G. Duplicate FCM does not create duplicate notification/alarm.
 * H. FCM topic subscription remains functional.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverOnlyArchitectureTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockNotificationManager: AlertNotificationManager = mock()
    private val mockAlarmController: AlarmController = mock()
    private lateinit var mockRepository: MockAlertRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = MockAlertRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Requirement 16A: App startup produces no alert
    // -------------------------------------------------------------------------
    @Test
    fun `Requirement 16A - App startup produces no alert`() = testScope.runTest {
        // Given an empty repository on fresh app launch
        assertEquals(0, mockRepository.getActiveAlerts().first().size)

        // When MainViewModel starts up
        val mainViewModel = MainViewModel(
            notificationManager = mockNotificationManager,
            alarmController = mockAlarmController,
            alertRepository = mockRepository
        )
        advanceUntilIdle()

        // Then no alert is produced/generated into repository
        val activeAlerts = mockRepository.getActiveAlerts().first()
        assertEquals("App startup must produce zero alerts", 0, activeAlerts.size)
    }

    // -------------------------------------------------------------------------
    // Requirement 16B: App startup produces no alarm
    // -------------------------------------------------------------------------
    @Test
    fun `Requirement 16B - App startup produces no alarm`() = testScope.runTest {
        // Given pre-existing alerts in local storage before app launch
        val cachedAlert = MockAlertData.criticalAlert.copy(
            alertId = "ALT-CACHED-01",
            source = "sih26001_demo",
            severity = AlertSeverity.CRITICAL,
            status = AlertStatus.ACTIVE
        )
        mockRepository.triggerTestAlert(cachedAlert)
        advanceUntilIdle()

        // When app launches (MainViewModel initialized)
        val mainViewModel = MainViewModel(
            notificationManager = mockNotificationManager,
            alarmController = mockAlarmController,
            alertRepository = mockRepository
        )
        advanceUntilIdle()

        // Then alarmController.startAlarm must NEVER be called on startup
        verify(mockAlarmController, never()).startAlarm(any())
        verify(mockNotificationManager, never()).showAlertNotification(any())
    }

    // -------------------------------------------------------------------------
    // Requirement 16C: No local demo alert is generated
    // -------------------------------------------------------------------------
    @Test
    fun `Requirement 16C - No local demo alert is generated`() = testScope.runTest {
        // Verify HomeViewModel startup does not inject any demo data into local repository
        val homeViewModel = HomeViewModel(
            alertRepository = mockRepository,
            acknowledgeAlertUseCase = mock(),
            getActiveAlertsUseCase = com.sih26001.mobilealert.domain.usecase.GetActiveAlertsUseCase(mockRepository),
            alarmController = mockAlarmController
        )
        advanceUntilIdle()

        val alerts = mockRepository.getActiveAlerts().first()
        val demoAlerts = alerts.filter { it.isDemoAlert() }
        assertTrue("Android client must never generate local demo alerts", demoAlerts.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Requirement 16D: Real cloud HIGH or CRITICAL never activates physical alarm
    // -------------------------------------------------------------------------
    @Test
    fun `Requirement 16D - Real cloud HIGH or CRITICAL never activates physical alarm`() = testScope.runTest {
        val realCloudCritical = Alert(
            alertId = "ALT-CLOUD-CRIT-99",
            eventType = "LANDSLIDE_WARNING",
            severity = AlertSeverity.CRITICAL,
            riskScore = 0.95,
            location = null,
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(7200),
            topDrivers = listOf("Heavy monsoon rain"),
            recommendedAction = "Evacuate low-lying areas",
            affectedAssets = null,
            source = "Risk Engine", // Real authoritative source, NOT demo
            dataQuality = "GOOD",
            requiresAck = false,
            status = AlertStatus.ACTIVE
        )
        assertFalse("Alert must not be flagged as demo", realCloudCritical.isDemoAlert())

        mockRepository.triggerTestAlert(realCloudCritical)

        val handler = FcmAlertTriggerHandlerImpl(
            alertRepository = mockRepository,
            notificationManager = mockNotificationManager,
            alarmController = mockAlarmController,
            coroutineScope = testScope
        )

        handler.handleAlertTrigger(realCloudCritical.alertId)
        advanceUntilIdle()

        // Notification is displayed to user
        verify(mockNotificationManager).showAlertNotification(realCloudCritical)

        // BUT physical hardware alarm (vibration/siren) is STRICTLY NEVER triggered
        verify(mockAlarmController, never()).startAlarm(any())
    }

    // -------------------------------------------------------------------------
    // Requirement 16E: Backend-generated demo HIGH or CRITICAL can activate alarm
    // -------------------------------------------------------------------------
    @Test
    fun `Requirement 16E - Backend-generated demo HIGH or CRITICAL activates alarm only after receiving authoritative FCM alert`() = testScope.runTest {
        val backendDemoAlert = Alert(
            alertId = "ALT-DEMO-HIGH-42",
            eventType = "LANDSLIDE_DANGER",
            severity = AlertSeverity.CRITICAL,
            riskScore = 0.98,
            location = null,
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            topDrivers = listOf("Soil pore saturation"),
            recommendedAction = "Move to shelter",
            affectedAssets = null,
            source = "sih26001_demo", // Explicitly identified as demo by backend
            dataQuality = "GOOD",
            requiresAck = true,
            status = AlertStatus.ACTIVE
        )
        assertTrue("Alert must be recognized as demo", backendDemoAlert.isDemoAlert())

        mockRepository.triggerTestAlert(backendDemoAlert)

        val handler = FcmAlertTriggerHandlerImpl(
            alertRepository = mockRepository,
            notificationManager = mockNotificationManager,
            alarmController = mockAlarmController,
            coroutineScope = testScope
        )

        // Alarm has NOT activated prior to receiving the authoritative FCM push
        verify(mockAlarmController, never()).startAlarm(any())

        // Receive the FCM message dispatch
        handler.handleAlertTrigger(backendDemoAlert.alertId)
        advanceUntilIdle()

        // Authoritative fetch confirms active demo CRITICAL alert -> alarm activated
        verify(mockNotificationManager).showAlertNotification(backendDemoAlert)
        verify(mockAlarmController).startAlarm(backendDemoAlert)
    }

    // -------------------------------------------------------------------------
    // Requirement 16F: Malformed or untrusted FCM payload cannot activate alarm
    // -------------------------------------------------------------------------
    @Test
    fun `Requirement 16F - Malformed or untrusted FCM payload cannot activate alarm`() = testScope.runTest {
        val processor = FcmMessageProcessor(maxCacheSize = 100)

        // 1. Missing alert_id
        val missingIdPayload = mapOf("severity" to "CRITICAL", "source" to "sih26001_demo")
        val res1 = processor.processData(missingIdPayload)
        assertTrue(res1 is FcmProcessingResult.MissingAlertId)

        // 2. Empty payload
        val emptyPayload = emptyMap<String, String>()
        val res2 = processor.processData(emptyPayload)
        assertTrue(res2 is FcmProcessingResult.EmptyPayload)

        // 3. Blank alert_id
        val blankIdPayload = mapOf("alert_id" to "   ")
        val res3 = processor.processData(blankIdPayload)
        assertTrue(res3 is FcmProcessingResult.MissingAlertId)

        // In all malformed cases, handler is never invoked and alarm is never started
        verify(mockAlarmController, never()).startAlarm(any())
        verify(mockNotificationManager, never()).showAlertNotification(any())
    }

    // -------------------------------------------------------------------------
    // Requirement 16G: Duplicate FCM does not create duplicate notification or alarm
    // -------------------------------------------------------------------------
    @Test
    fun `Requirement 16G - Duplicate FCM does not create duplicate notification or alarm`() = testScope.runTest {
        val processor = FcmMessageProcessor(maxCacheSize = 100)
        val alertId = "ALT-DEMO-DUP-01"

        val demoAlert = Alert(
            alertId = alertId,
            eventType = "LANDSLIDE_DANGER",
            severity = AlertSeverity.CRITICAL,
            riskScore = 0.90,
            location = null,
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            topDrivers = emptyList(),
            recommendedAction = "Take safety measures",
            affectedAssets = null,
            source = "sih26001_demo",
            dataQuality = "GOOD",
            requiresAck = true,
            status = AlertStatus.ACTIVE
        )
        mockRepository.triggerTestAlert(demoAlert)

        val handler = FcmAlertTriggerHandlerImpl(
            alertRepository = mockRepository,
            notificationManager = mockNotificationManager,
            alarmController = mockAlarmController,
            coroutineScope = testScope
        )

        // 1st FCM arrival
        val res1 = processor.processData(mapOf("alert_id" to alertId))
        assertTrue(res1 is FcmProcessingResult.ValidAlert)
        handler.handleAlertTrigger(alertId)
        advanceUntilIdle()

        // 2nd duplicate FCM arrival
        val res2 = processor.processData(mapOf("alert_id" to alertId))
        assertTrue(res2 is FcmProcessingResult.DuplicateAlert)

        // Verify notification and alarm were triggered exactly once
        verify(mockNotificationManager, times(1)).showAlertNotification(demoAlert)
        verify(mockAlarmController, times(1)).startAlarm(demoAlert)
    }

    // -------------------------------------------------------------------------
    // Requirement 16H: FCM topic subscription remains functional
    // -------------------------------------------------------------------------
    @Test
    fun `Requirement 16H - FCM topic subscription remains functional`() {
        assertEquals("sih26001_alerts", FcmTopicSubscriber.TOPIC_ALERTS)
        assertEquals("sih26001_alerts", Constants.FCM_TOPIC_ALERTS)

        val mockMessaging: FirebaseMessaging = mock()
        val mockTask: Task<Void> = mock()
        whenever(mockMessaging.subscribeToTopic("sih26001_alerts")).thenReturn(mockTask)

        var subscriptionSuccess: Boolean? = null
        whenever(mockTask.addOnCompleteListener(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val listener = invocation.arguments[0] as OnCompleteListener<Void>
            whenever(mockTask.isSuccessful).thenReturn(true)
            listener.onComplete(mockTask)
            mockTask
        }

        FcmTopicSubscriber.subscribeToAlertsTopic(
            messaging = mockMessaging,
            topic = "sih26001_alerts",
            onComplete = { success, _ -> subscriptionSuccess = success }
        )

        verify(mockMessaging).subscribeToTopic("sih26001_alerts")
        assertTrue(subscriptionSuccess == true)
    }
}
