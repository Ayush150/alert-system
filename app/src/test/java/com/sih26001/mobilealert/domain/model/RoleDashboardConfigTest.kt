package com.sih26001.mobilealert.domain.model

import android.content.Context
import android.content.SharedPreferences
import com.sih26001.mobilealert.core.alarm.AlarmController
import com.sih26001.mobilealert.core.demo.DemoProtocolManager
import com.sih26001.mobilealert.data.preferences.RolePreferences
import com.sih26001.mobilealert.presentation.roleselection.RoleSelectionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class RoleDashboardConfigTest {

    // -------------------------------------------------------------------------
    // 1. Each UserRole resolves to the correct dashboard configuration
    // -------------------------------------------------------------------------

    @Test
    fun `citizen role resolves to correct configuration and priorities`() {
        val config = RoleDashboardConfig.forRole(UserRole.CITIZEN)

        assertEquals(UserRole.CITIZEN, config.role)
        assertEquals("Stay informed about hazards and follow official warnings.", config.contextText)
        assertEquals(4, config.priorities.size)

        val titles = config.priorities.map { it.title }
        assertEquals(
            listOf("Current Risk", "Active Alerts", "Safe Places", "Report Hazard"),
            titles
        )

        assertEquals(PriorityActionType.CURRENT_RISK, config.priorities[0].actionType)
        assertEquals(PriorityActionType.ACTIVE_ALERTS, config.priorities[1].actionType)
        assertEquals(PriorityActionType.SAFE_PLACES, config.priorities[2].actionType)
        assertEquals(PriorityActionType.UNAVAILABLE, config.priorities[3].actionType)
    }

    @Test
    fun `police role resolves to correct configuration and priorities`() {
        val config = RoleDashboardConfig.forRole(UserRole.POLICE)

        assertEquals(UserRole.POLICE, config.role)
        assertEquals("Monitor affected areas and coordinate local emergency response.", config.contextText)
        assertEquals(4, config.priorities.size)

        val titles = config.priorities.map { it.title }
        assertEquals(
            listOf("Active Alerts", "Affected Areas", "Road / Infrastructure Exposure", "Response Coordination"),
            titles
        )

        assertEquals(PriorityActionType.ACTIVE_ALERTS, config.priorities[0].actionType)
        assertEquals(PriorityActionType.UNAVAILABLE, config.priorities[1].actionType)
        assertEquals(PriorityActionType.UNAVAILABLE, config.priorities[2].actionType)
        assertEquals(PriorityActionType.UNAVAILABLE, config.priorities[3].actionType)
    }

    @Test
    fun `rescue team role resolves to correct configuration and priorities`() {
        val config = RoleDashboardConfig.forRole(UserRole.RESCUE_TEAM)

        assertEquals(UserRole.RESCUE_TEAM, config.role)
        assertEquals("Prioritise rescue operations using current risk intelligence.", config.contextText)
        assertEquals(4, config.priorities.size)

        val titles = config.priorities.map { it.title }
        assertEquals(
            listOf("Critical / High Risk Zones", "Active Alerts", "Rescue Priorities", "Affected Infrastructure"),
            titles
        )

        assertEquals(PriorityActionType.ACTIVE_ALERTS, config.priorities[0].actionType)
        assertEquals(PriorityActionType.ACTIVE_ALERTS, config.priorities[1].actionType)
        assertEquals(PriorityActionType.UNAVAILABLE, config.priorities[2].actionType)
        assertEquals(PriorityActionType.UNAVAILABLE, config.priorities[3].actionType)
    }

    @Test
    fun `district administration role resolves to correct configuration and priorities`() {
        val config = RoleDashboardConfig.forRole(UserRole.DISTRICT_ADMINISTRATION)

        assertEquals(UserRole.DISTRICT_ADMINISTRATION, config.role)
        assertEquals("Monitor regional conditions and coordinate emergency response.", config.contextText)
        assertEquals(4, config.priorities.size)

        val titles = config.priorities.map { it.title }
        assertEquals(
            listOf("Regional Risk Overview", "Active Alerts", "Infrastructure Exposure", "Response Status"),
            titles
        )

        assertEquals(PriorityActionType.CURRENT_RISK, config.priorities[0].actionType)
        assertEquals(PriorityActionType.ACTIVE_ALERTS, config.priorities[1].actionType)
        assertEquals(PriorityActionType.UNAVAILABLE, config.priorities[2].actionType)
        assertEquals(PriorityActionType.UNAVAILABLE, config.priorities[3].actionType)
    }

    @Test
    fun `emergency services role resolves to correct configuration and priorities`() {
        val config = RoleDashboardConfig.forRole(UserRole.EMERGENCY_SERVICES)

        assertEquals(UserRole.EMERGENCY_SERVICES, config.role)
        assertEquals("Coordinate emergency operations using live risk intelligence.", config.contextText)
        assertEquals(4, config.priorities.size)

        val titles = config.priorities.map { it.title }
        assertEquals(
            listOf("Active Alerts", "High/Critical Zones", "Infrastructure Exposure", "Response Status"),
            titles
        )

        assertEquals(PriorityActionType.ACTIVE_ALERTS, config.priorities[0].actionType)
        assertEquals(PriorityActionType.ACTIVE_ALERTS, config.priorities[1].actionType)
        assertEquals(PriorityActionType.UNAVAILABLE, config.priorities[2].actionType)
        assertEquals(PriorityActionType.UNAVAILABLE, config.priorities[3].actionType)
    }

    // -------------------------------------------------------------------------
    // 2. Dashboard configuration does NOT contain fabricated numeric data
    // -------------------------------------------------------------------------

    @Test
    fun `dashboard configurations do not contain fabricated numeric data`() {
        val allowedPlaceholders = setOf("Data unavailable", "No active incidents", null)

        UserRole.entries.forEach { role ->
            val config = RoleDashboardConfig.forRole(role)

            config.priorities.forEach { priority ->
                // Check placeholder text
                assertTrue(
                    "Priority '${priority.title}' for role $role has invalid placeholder: '${priority.placeholderStatus}'",
                    priority.placeholderStatus in allowedPlaceholders
                )

                // Placeholder must not contain any digits (no fake casualty, risk, or incident counts)
                priority.placeholderStatus?.let { status ->
                    assertFalse(
                        "Placeholder '$status' in priority '${priority.title}' contains fabricated numbers",
                        status.any { it.isDigit() }
                    )
                }

                // Priority title must not contain fabricated numeric metrics
                assertFalse(
                    "Title '${priority.title}' contains fabricated numbers",
                    priority.title.any { it.isDigit() }
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. Changing role updates the displayed configuration
    // -------------------------------------------------------------------------

    @Test
    fun `changing role updates the configuration and context text`() {
        var currentRole = UserRole.CITIZEN
        var config = RoleDashboardConfig.forRole(currentRole)

        assertEquals("Stay informed about hazards and follow official warnings.", config.contextText)
        assertEquals("Current Risk", config.priorities.first().title)

        // Switch to Rescue Team
        currentRole = UserRole.RESCUE_TEAM
        config = RoleDashboardConfig.forRole(currentRole)

        assertEquals("Prioritise rescue operations using current risk intelligence.", config.contextText)
        assertEquals("Critical / High Risk Zones", config.priorities.first().title)

        // Switch to Police
        currentRole = UserRole.POLICE
        config = RoleDashboardConfig.forRole(currentRole)

        assertEquals("Monitor affected areas and coordinate local emergency response.", config.contextText)
        assertTrue(config.priorities.any { it.title == "Response Coordination" })
    }

    // -------------------------------------------------------------------------
    // 4. Role selection still persists
    // -------------------------------------------------------------------------

    @Test
    fun `role preferences persists and retrieves role correctly`() {
        val mockContext: Context = mock()
        val mockSharedPreferences: SharedPreferences = mock()
        val mockEditor: SharedPreferences.Editor = mock()
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockSharedPreferences)
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.remove(any())).thenReturn(mockEditor)

        val rolePrefs = RolePreferences(mockContext)

        // Save Police
        rolePrefs.setSelectedRole(UserRole.POLICE)
        verify(mockEditor).putString("selected_role", "POLICE")

        // Retrieve Police
        whenever(mockSharedPreferences.getString("selected_role", null)).thenReturn("POLICE")
        assertEquals(UserRole.POLICE, rolePrefs.getSelectedRole())
        assertTrue(rolePrefs.hasSelectedRole())

        // Clear role
        rolePrefs.clearRole()
        verify(mockEditor).remove("selected_role")
    }

    // -------------------------------------------------------------------------
    // 5. Role selection does not interact with AlarmController or DemoProtocolManager
    // -------------------------------------------------------------------------

    @Test
    fun `role selection does not interact with AlarmController or DemoProtocolManager`() {
        val mockAlarmController: AlarmController = mock()
        val mockDemoProtocolManager: DemoProtocolManager = mock()
        val mockRolePrefs: RolePreferences = mock()

        val viewModel = RoleSelectionViewModel(rolePreferences = mockRolePrefs)

        // 1. Select a role
        viewModel.selectRole(UserRole.RESCUE_TEAM)

        // 2. Confirm selection
        viewModel.confirmSelection()

        // 3. Resolve dashboard configuration for the selected role
        val selectedRole = viewModel.uiState.value.selectedRole
        assertNotNull(selectedRole)
        val config = RoleDashboardConfig.forRole(selectedRole!!)
        assertEquals(UserRole.RESCUE_TEAM, config.role)

        // 4. STRICT VERIFICATION:
        // Role selection MUST NOT interact with AlarmController or DemoProtocolManager
        verifyNoInteractions(mockAlarmController)
        verifyNoInteractions(mockDemoProtocolManager)
    }
}
