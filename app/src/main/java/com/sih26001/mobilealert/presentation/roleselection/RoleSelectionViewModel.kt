package com.sih26001.mobilealert.presentation.roleselection

import androidx.lifecycle.ViewModel
import com.sih26001.mobilealert.data.preferences.RolePreferences
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RoleSelectionUiState(
    val selectedRole: UserRole? = null,
    val isConfirmed: Boolean = false
)

class RoleSelectionViewModel(
    private val rolePreferences: RolePreferences = DependencyContainer.rolePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleSelectionUiState())
    val uiState: StateFlow<RoleSelectionUiState> = _uiState.asStateFlow()

    /**
     * Called when the user taps a role card.
     */
    fun selectRole(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    /**
     * Called when the user taps "Continue". Persists the role and marks confirmation.
     */
    fun confirmSelection() {
        val role = _uiState.value.selectedRole ?: return
        rolePreferences.setSelectedRole(role)
        _uiState.update { it.copy(isConfirmed = true) }
    }
}
