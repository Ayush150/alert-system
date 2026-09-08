package com.sih26001.mobilealert.domain.model

/**
 * Valid alert severities defined by the SIH26001 alert contract.
 * Maps directly to system notification priority channels in later phases.
 */
enum class AlertSeverity {
    NORMAL,
    HIGH,
    CRITICAL
}
