package com.sih26001.mobilealert.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.sih26001.mobilealert.domain.model.UserRole

/**
 * Lightweight persistence for the selected demo role using SharedPreferences.
 * No DataStore dependency required — avoids adding new libraries for a simple key-value store.
 */
class RolePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "sixth_sense_role_prefs"
        private const val KEY_SELECTED_ROLE = "selected_role"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the persisted [UserRole], or null if no role has been selected yet (first launch).
     */
    fun getSelectedRole(): UserRole? {
        val key = prefs.getString(KEY_SELECTED_ROLE, null)
        return UserRole.fromKey(key)
    }

    /**
     * Persists the user's role selection.
     */
    fun setSelectedRole(role: UserRole) {
        prefs.edit().putString(KEY_SELECTED_ROLE, role.name).apply()
    }

    /**
     * Clears the persisted role, forcing the role selection screen to appear again.
     */
    fun clearRole() {
        prefs.edit().remove(KEY_SELECTED_ROLE).apply()
    }

    /**
     * Returns true if a role has been selected and persisted.
     */
    fun hasSelectedRole(): Boolean = getSelectedRole() != null
}
