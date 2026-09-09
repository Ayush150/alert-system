package com.sih26001.mobilealert.presentation.activealarm

import androidx.lifecycle.SavedStateHandle
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.repository.AlertRepository
import com.sih26001.mobilealert.domain.usecase.AcknowledgeAlertUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
class ActiveAlarmViewModelTest {

    private val alertRepository: AlertRepository = mock()
    private val acknowledgeAlertUseCase: AcknowledgeAlertUseCase = mock()
    private val testDispatcher = StandardTestDispatcher()

    private val alertId = "ALT-TEST-1"
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ActiveAlarmViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = SavedStateHandle(mapOf("alertId" to alertId))

        whenever(alertRepository.getAlertById(alertId)).thenReturn(flowOf(createAlert(alertId, AlertStatus.ACTIVE)))
        whenever(alertRepository.observePendingAckIds()).thenReturn(flowOf(emptySet()))
        whenever(alertRepository.observeAckSyncStatus(alertId)).thenReturn(flowOf(null))

        viewModel = ActiveAlarmViewModel(
            savedStateHandle = savedStateHandle,
            alertRepository = alertRepository,
            acknowledgeAlertUseCase = acknowledgeAlertUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `silenceAlert delegates to repository silenceAlert and never acknowledges`() = runTest {
        whenever(alertRepository.silenceAlert(alertId)).thenReturn(Result.success(Unit))

        viewModel.silenceAlert()
        advanceUntilIdle()

        verify(alertRepository).silenceAlert(alertId)
        verify(acknowledgeAlertUseCase, never()).invoke(any())
    }

    @Test
    fun `acknowledgeAlert delegates to acknowledgeAlertUseCase`() = runTest {
        whenever(acknowledgeAlertUseCase.invoke(alertId)).thenReturn(Result.success(Unit))

        viewModel.acknowledgeAlert()
        advanceUntilIdle()

        verify(acknowledgeAlertUseCase).invoke(alertId)
        assertNull(viewModel.ackError.value)
    }

    @Test
    fun `acknowledgeAlert failure updates ackError state`() = runTest {
        whenever(acknowledgeAlertUseCase.invoke(alertId))
            .thenReturn(Result.failure(NoSuchElementException("Alert not found")))

        viewModel.acknowledgeAlert()
        advanceUntilIdle()

        verify(acknowledgeAlertUseCase).invoke(alertId)
        assertEquals("Alert not found", viewModel.ackError.value)
    }

    @Test
    fun `isAckPending is true when alertId is in pendingAckIds`() = runTest {
        whenever(alertRepository.observePendingAckIds()).thenReturn(flowOf(setOf(alertId)))

        val vmWithPending = ActiveAlarmViewModel(
            savedStateHandle = savedStateHandle,
            alertRepository = alertRepository,
            acknowledgeAlertUseCase = acknowledgeAlertUseCase
        )

        val job = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            vmWithPending.isAckPending.collect {}
        }
        advanceUntilIdle()
        assertTrue(vmWithPending.isAckPending.value)
        job.cancel()
    }

    @Test
    fun `ackSyncStatus reflects repository observed status`() = runTest {
        whenever(alertRepository.observeAckSyncStatus(alertId)).thenReturn(flowOf(com.sih26001.mobilealert.data.local.AckSyncStatus.FAILED))

        val vmWithStatus = ActiveAlarmViewModel(
            savedStateHandle = savedStateHandle,
            alertRepository = alertRepository,
            acknowledgeAlertUseCase = acknowledgeAlertUseCase
        )

        val job = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            vmWithStatus.ackSyncStatus.collect {}
        }
        advanceUntilIdle()
        assertEquals(com.sih26001.mobilealert.data.local.AckSyncStatus.FAILED, vmWithStatus.ackSyncStatus.value)
        job.cancel()
    }

    private fun createAlert(id: String, status: AlertStatus): Alert {
        return Alert(
            alertId = id,
            eventType = "LANDSLIDE_RISK",
            severity = AlertSeverity.CRITICAL,
            riskScore = 85.0,
            location = null,
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600),
            topDrivers = null,
            recommendedAction = "Take shelter",
            affectedAssets = null,
            source = "test",
            dataQuality = "GOOD",
            requiresAck = true,
            status = status
        )
    }
}
