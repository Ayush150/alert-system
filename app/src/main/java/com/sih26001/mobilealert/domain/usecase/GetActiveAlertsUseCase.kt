package com.sih26001.mobilealert.domain.usecase

import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow

class GetActiveAlertsUseCase(
    private val alertRepository: AlertRepository
) {
    operator fun invoke(): Flow<List<Alert>> {
        return alertRepository.getActiveAlerts()
    }
}
