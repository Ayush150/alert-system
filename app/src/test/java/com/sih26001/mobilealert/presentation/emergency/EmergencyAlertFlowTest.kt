package com.sih26001.mobilealert.presentation.emergency

import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.Location
import com.sih26001.mobilealert.domain.repository.AlertRepository
import com.sih26001.mobilealert.domain.usecase.AcknowledgeAlertUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyAlertFlowTest {

    private val alertRepository: AlertRepository = mock()
    private val acknowledgeAlertUseCase: AcknowledgeAlertUseCase = mock()
    private val alarmController: AlarmController = mock()
    private val testDispatcher = StandardTestDispatcher()

    private val sirenStateFlow = MutableStateFlow(true)
    private val alertId = "ALT-EMERGENCY-001"

    private lateinit var viewModel: EmergencyAlertViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(alarmController.isSirenActive).thenReturn(sirenStateFlow)
        whenever(alertRepository.getAlertById(alertId)).thenReturn(
            flowOf(createDemoEmergencyAlert(alertId, AlertStatus.ACTIVE))
        )
        whenever(alertRepository.observePendingAckIds()).thenReturn(flowOf(emptySet()))

        viewModel = EmergencyAlertViewModel(
            alertId = alertId,
            alertRepository = alertRepository,
            acknowledgeAlertUseCase = acknowledgeAlertUseCase,
            alarmController = alarmController
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `silenceSiren stops audio vibration and silences locally without acknowledging`() = runTest {
        whenever(alertRepository.silenceAlert(alertId)).thenReturn(Result.success(Unit))

        viewModel.silenceSiren()
        advanceUntilIdle()

        // Verifies siren was silenced via controller
        verify(alarmController).silenceAlarm(alertId)
        // Verifies repository was instructed to silence
        verify(alertRepository).silenceAlert(alertId)
        // CRITICAL REQUIREMENT: SILENCE MUST NEVER ACKNOWLEDGE
        verify(acknowledgeAlertUseCase, never()).invoke(any())
    }

    @Test
    fun `acknowledgeAlert stops siren and executes authoritative acknowledgement`() = runTest {
        whenever(acknowledgeAlertUseCase(alertId)).thenReturn(Result.success(Unit))

        viewModel.acknowledgeAlert()
        advanceUntilIdle()

        // Verifies siren is stopped when acknowledging
        verify(alarmController).stopAlarm()
        // Verifies authoritative ACK is submitted
        verify(acknowledgeAlertUseCase).invoke(alertId)
        // Verifies silenceAlert was not invoked
        verify(alertRepository, never()).silenceAlert(any())
    }

    @Test
    fun `isSirenActive accurately mirrors AlarmController state`() = runTest {
        assertTrue(viewModel.isSirenActive.value)

        sirenStateFlow.value = false
        advanceUntilIdle()

        assertFalse(viewModel.isSirenActive.value)
    }

    @Test
    fun `destination state resolves safe place with demo indicators for demo alerts`() = runTest {
        val dest = viewModel.destination.value
        assertTrue("Demo alert destination must have isDemo = true", dest.isDemo)
        assertTrue(dest.name.contains("Relief Centre"))
        assertEquals("1.8 km", dest.distance)
        assertEquals("Approx. 7 min", dest.estimatedTime)
    }

    private fun createDemoEmergencyAlert(
        id: String,
        status: AlertStatus
    ): Alert {
        return Alert(
            alertId = id,
            eventType = "demo_emergency",
            severity = AlertSeverity.HIGH,
            status = status,
            location = Location(
                name = "Shillong Sector 4",
                latitude = 25.5788,
                longitude = 91.8933
            ),
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            recommendedAction = "EVACUATE IMMEDIATELY",
            source = "sih26001_demo"
        )
    }
}
