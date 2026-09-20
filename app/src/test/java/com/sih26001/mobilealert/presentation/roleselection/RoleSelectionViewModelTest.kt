package com.sih26001.mobilealert.presentation.roleselection

import com.sih26001.mobilealert.data.preferences.RolePreferences
import com.sih26001.mobilealert.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import org.mockito.kotlin.any

class RoleSelectionViewModelTest {

    private lateinit var mockPrefs: RolePreferences
    private lateinit var viewModel: RoleSelectionViewModel

    @Before
    fun setUp() {
        mockPrefs = mock()
        viewModel = RoleSelectionViewModel(rolePreferences = mockPrefs)
    }

    @Test
    fun `initial state has no selected role and is not confirmed`() {
        val state = viewModel.uiState.value
        assertNull(state.selectedRole)
        assertFalse(state.isConfirmed)
    }

    @Test
    fun `selectRole updates selected role in state`() {
        viewModel.selectRole(UserRole.CITIZEN)
        assertEquals(UserRole.CITIZEN, viewModel.uiState.value.selectedRole)
    }

    @Test
    fun `selectRole can change selection`() {
        viewModel.selectRole(UserRole.CITIZEN)
        viewModel.selectRole(UserRole.POLICE)
        assertEquals(UserRole.POLICE, viewModel.uiState.value.selectedRole)
    }

    @Test
    fun `confirmSelection does nothing if no role selected`() {
        viewModel.confirmSelection()
        assertFalse(viewModel.uiState.value.isConfirmed)
        verify(mockPrefs, never()).setSelectedRole(any())
    }

    @Test
    fun `confirmSelection persists role and marks confirmed`() {
        viewModel.selectRole(UserRole.RESCUE_TEAM)
        viewModel.confirmSelection()

        assertTrue(viewModel.uiState.value.isConfirmed)
        verify(mockPrefs).setSelectedRole(UserRole.RESCUE_TEAM)
    }

    @Test
    fun `confirmSelection persists the most recently selected role`() {
        viewModel.selectRole(UserRole.CITIZEN)
        viewModel.selectRole(UserRole.EMERGENCY_SERVICES)
        viewModel.confirmSelection()

        verify(mockPrefs).setSelectedRole(UserRole.EMERGENCY_SERVICES)
        assertTrue(viewModel.uiState.value.isConfirmed)
    }

    @Test
    fun `selecting role does not trigger confirmation`() {
        viewModel.selectRole(UserRole.DISTRICT_ADMINISTRATION)
        assertFalse(viewModel.uiState.value.isConfirmed)
        verify(mockPrefs, never()).setSelectedRole(any())
    }
}
