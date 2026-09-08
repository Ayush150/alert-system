package com.sih26001.mobilealert.data.remote.validation

import com.sih26001.mobilealert.data.remote.dto.AlertDto
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Lightweight validator to verify raw incoming alert payloads safely.
 *
 * Rejects invalid, incomplete, or corrupt data without throwing uncaught exceptions,
 * without attempting to fabricate missing values, and without crashing the app.
 */
object AlertValidator {

    sealed interface Result {
        data class Valid(val dto: AlertDto) : Result
        data class Invalid(val reasons: List<String>) : Result
    }

    fun validate(dto: AlertDto): Result {
        val errors = mutableListOf<String>()

        // 1. alert_id must be non-null and non-blank
        if (dto.alert_id.isNull(dto.alert_id.isNullOrBlank())) {
            errors.add("alert_id is missing or blank")
        }

        // 2. event_type must be non-null and non-blank
        if (dto.event_type.isNullOrBlank()) {
            errors.add("event_type is missing or blank")
        }

        // 3. severity must match NORMAL, HIGH, or CRITICAL
        if (dto.severity.isNullOrBlank()) {
            errors.add("severity is missing")
        } else {
            val normalizedSeverity = dto.severity.uppercase()
            val isValidSeverity = AlertSeverity.values().any { it.name == normalizedSeverity }
            if (!isValidSeverity) {
                errors.add("severity '${dto.severity}' is not a recognized AlertSeverity (expected NORMAL, HIGH, CRITICAL)")
            }
        }

        // 4. issued_at must be a valid ISO-8601 timestamp
        if (dto.issued_at.isNullOrBlank()) {
            errors.add("issued_at timestamp is missing")
        } else {
            try {
                Instant.parse(dto.issued_at)
            } catch (e: DateTimeParseException) {
                errors.add("issued_at '${dto.issued_at}' is not a valid ISO-8601 timestamp")
            }
        }

        // 5. expires_at (if present) must be a valid ISO-8601 timestamp
        if (!dto.expires_at.isNullOrBlank()) {
            try {
                Instant.parse(dto.expires_at)
            } catch (e: DateTimeParseException) {
                errors.add("expires_at '${dto.expires_at}' is not a valid ISO-8601 timestamp")
            }
        }

        // 6. location coordinates (if present) must be within valid geographic bounds
        dto.location?.let { loc ->
            loc.latitude?.let { lat ->
                if (lat < -90.0 || lat > 90.0) {
                    errors.add("latitude $lat is out of valid range [-90.0, 90.0]")
                }
            }
            loc.longitude?.let { lon ->
                if (lon < -180.0 || lon > 180.0) {
                    errors.add("longitude $lon is out of valid range [-180.0, 180.0]")
                }
            }
        }

        // 7. status (if present) must be a recognized AlertStatus
        if (!dto.status.isNullOrBlank()) {
            val normalizedStatus = dto.status.uppercase()
            val isValidStatus = AlertStatus.values().any { it.name == normalizedStatus }
            if (!isValidStatus) {
                errors.add("status '${dto.status}' is not a recognized AlertStatus")
            }
        }

        return if (errors.isEmpty()) {
            Result.Valid(dto)
        } else {
            Result.Invalid(errors)
        }
    }

    private fun String?.isNull(condition: Boolean): Boolean = condition
}
