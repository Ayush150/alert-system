package com.sih26001.mobilealert.core.util

enum class Environment {
    DEBUG, STAGING, PRODUCTION
}

object EnvironmentConfig {
    // Determine the current environment based on BuildConfig.DEBUG or manual override
    // For Phase 5 networking foundation, we are forcing DEBUG for local mock testing.
    val CURRENT_ENV = Environment.DEBUG

    val BASE_URL: String
        get() = when (CURRENT_ENV) {
            // Address for accessing a mock server running on host machine from emulator via adb reverse
            Environment.DEBUG -> "http://127.0.0.1:8080/"
            
            // Placeholders for staging and production backends
            Environment.STAGING -> "https://staging.api.placeholder.com/"
            Environment.PRODUCTION -> "https://api.placeholder.com/"
        }
}
