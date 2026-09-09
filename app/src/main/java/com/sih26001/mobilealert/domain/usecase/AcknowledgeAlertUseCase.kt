package com.sih26001.mobilealert.domain.usecase

import com.sih26001.mobilealert.domain.repository.AlertRepository

/**
 * Domain use case for acknowledging an alert.
 * Validates the alert identifier and coordinates with the repository.
 *
 * Silencing an alert (local alarm mute) does NOT acknowledge an alert.
 * Acknowledging is an operational event that must be persisted locally and queued for backend synchronization.
 */
class AcknowledgeAlertUseCase(
    private val alertRepository: AlertRepository
) {
    suspend operator fun invoke(alertId: String): Result<Unit> {
        if (alertId.isBlank()) {
            return Result.failure(IllegalArgumentException("alertId cannot be blank"))
        }
        return alertRepository.acknowledgeAlert(alertId.trim())
    }
}
