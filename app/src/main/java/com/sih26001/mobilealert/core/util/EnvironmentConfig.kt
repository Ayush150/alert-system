package com.sih26001.mobilealert.core.util

enum class Environment {
    DEBUG, STAGING, PRODUCTION
}

object EnvironmentConfig {
    // Determine the current environment based on BuildConfig.DEBUG or manual override
    // For Phase 5 networking foundation, we are forcing DEBUG for local mock testing.
    val CURRENT_ENV = Environment.PRODUCTION

    val BASE_URL: String
        get() = when (CURRENT_ENV) {
            // Address for accessing a mock server running on host machine from emulator via adb reverse
            Environment.DEBUG -> "http://10.0.2.2:8000/"
            
            // Placeholders for staging and production backends
            Environment.STAGING -> "https://geoagent.campuscrafters.com/"
            Environment.PRODUCTION -> "https://geoagent.campuscrafters.com/"
        }
}
