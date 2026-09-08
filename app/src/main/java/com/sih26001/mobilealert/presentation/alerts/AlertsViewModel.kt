package com.sih26001.mobilealert.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sih26001.mobilealert.data.repository.AlertRepositoryImpl
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.usecase.GetActiveAlertsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AlertsUiState(
    val isLoading: Boolean = false,
    val alerts: List<Alert> = emptyList()
)

class AlertsViewModel(
    getActiveAlertsUseCase: GetActiveAlertsUseCase = GetActiveAlertsUseCase(AlertRepositoryImpl())
) : ViewModel() {

    val uiState: StateFlow<AlertsUiState> = getActiveAlertsUseCase()
        .map { alertList ->
            AlertsUiState(isLoading = false, alerts = alertList)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlertsUiState(isLoading = false, alerts = emptyList())
        )
}
