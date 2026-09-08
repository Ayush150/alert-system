package com.sih26001.mobilealert.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sih26001.mobilealert.data.repository.AlertRepositoryImpl
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.usecase.GetAlertHistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val isLoading: Boolean = false,
    val history: List<Alert> = emptyList()
)

class HistoryViewModel(
    getAlertHistoryUseCase: GetAlertHistoryUseCase = GetAlertHistoryUseCase(AlertRepositoryImpl())
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = getAlertHistoryUseCase()
        .map { historyList ->
            HistoryUiState(isLoading = false, history = historyList)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryUiState(isLoading = false, history = emptyList())
        )
}
