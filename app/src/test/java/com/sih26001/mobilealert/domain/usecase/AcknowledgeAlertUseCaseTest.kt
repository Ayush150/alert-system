package com.sih26001.mobilealert.domain.usecase

import com.sih26001.mobilealert.domain.repository.AlertRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AcknowledgeAlertUseCaseTest {

    private val alertRepository: AlertRepository = mock()
    private lateinit var useCase: AcknowledgeAlertUseCase

    @Before
    fun setUp() {
        useCase = AcknowledgeAlertUseCase(alertRepository)
    }

    @Test
    fun `invoke with blank alertId returns failure without invoking repository`() = runTest {
        val resultEmpty = useCase("")
        assertTrue(resultEmpty.isFailure)
        assertTrue(resultEmpty.exceptionOrNull() is IllegalArgumentException)

        val resultWhitespace = useCase("   ")
        assertTrue(resultWhitespace.isFailure)
        assertTrue(resultWhitespace.exceptionOrNull() is IllegalArgumentException)

        verify(alertRepository, never()).acknowledgeAlert(org.mockito.kotlin.any())
    }

    @Test
    fun `invoke with valid alertId delegates to repository and returns success`() = runTest {
        whenever(alertRepository.acknowledgeAlert("ALT-123")).thenReturn(Result.success(Unit))

        val result = useCase("ALT-123")

        assertTrue(result.isSuccess)
        verify(alertRepository).acknowledgeAlert("ALT-123")
    }

    @Test
    fun `invoke with valid alertId trims whitespace before delegation`() = runTest {
        whenever(alertRepository.acknowledgeAlert("ALT-123")).thenReturn(Result.success(Unit))

        val result = useCase("  ALT-123  ")

        assertTrue(result.isSuccess)
        verify(alertRepository).acknowledgeAlert("ALT-123")
    }

    @Test
    fun `invoke propagates repository failure safely`() = runTest {
        val exception = NoSuchElementException("Alert ALT-999 not found")
        whenever(alertRepository.acknowledgeAlert("ALT-999")).thenReturn(Result.failure(exception))

        val result = useCase("ALT-999")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(alertRepository).acknowledgeAlert("ALT-999")
    }
}
