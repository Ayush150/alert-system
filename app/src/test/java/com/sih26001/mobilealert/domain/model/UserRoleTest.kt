package com.sih26001.mobilealert.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UserRoleTest {

    @Test
    fun `all roles have non-empty display names`() {
        UserRole.entries.forEach { role ->
            assert(role.displayName.isNotBlank()) { "Role ${role.name} has blank displayName" }
        }
    }

    @Test
    fun `all roles have non-empty descriptions`() {
        UserRole.entries.forEach { role ->
            assert(role.description.isNotBlank()) { "Role ${role.name} has blank description" }
        }
    }

    @Test
    fun `all roles have non-empty emoji`() {
        UserRole.entries.forEach { role ->
            assert(role.emoji.isNotBlank()) { "Role ${role.name} has blank emoji" }
        }
    }

    @Test
    fun `fromKey returns correct role for valid key`() {
        UserRole.entries.forEach { role ->
            val result = UserRole.fromKey(role.name)
            assertNotNull(result)
            assertEquals(role, result)
        }
    }

    @Test
    fun `fromKey returns null for null key`() {
        assertNull(UserRole.fromKey(null))
    }

    @Test
    fun `fromKey returns null for blank key`() {
        assertNull(UserRole.fromKey(""))
        assertNull(UserRole.fromKey("   "))
    }

    @Test
    fun `fromKey returns null for invalid key`() {
        assertNull(UserRole.fromKey("INVALID_ROLE"))
        assertNull(UserRole.fromKey("citizen")) // case-sensitive
    }

    @Test
    fun `exactly five roles exist`() {
        assertEquals(5, UserRole.entries.size)
    }

    @Test
    fun `CITIZEN role has expected display name`() {
        assertEquals("Citizen", UserRole.CITIZEN.displayName)
    }

    @Test
    fun `RESCUE_TEAM role has expected display name`() {
        assertEquals("Rescue Team", UserRole.RESCUE_TEAM.displayName)
    }

    @Test
    fun `DISTRICT_ADMINISTRATION role has expected display name`() {
        assertEquals("District Administration", UserRole.DISTRICT_ADMINISTRATION.displayName)
    }
}
