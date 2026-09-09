package com.sih26001.mobilealert.data.repository

import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * In-memory Mock implementation of AlertRepository for Phase 3 testing.
 * Implements deduplication and exposes developer methods.
 */
class MockAlertRepository : AlertRepository {

    // Internal state holding all alerts
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())

    override fun getActiveAlerts(): Flow<List<Alert>> {
        return _alerts.map { list ->
            list.filter { it.status == AlertStatus.ACTIVE || it.status == AlertStatus.SILENCED }
                .sortedByDescending { it.issuedAt }
        }
    }

    override fun getAlertHistory(): Flow<List<Alert>> {
        return _alerts.map { list ->
            list.filter { it.status == AlertStatus.ACKNOWLEDGED || it.status == AlertStatus.EXPIRED }
                .sortedByDescending { it.issuedAt }
        }
    }

    override fun getAlertById(alertId: String): Flow<Alert?> {
        return _alerts.map { list ->
            list.find { it.alertId == alertId }
        }
    }

    private val _pendingAckIds = MutableStateFlow<Set<String>>(emptySet())

    override fun observePendingAckIds(): Flow<Set<String>> = _pendingAckIds

    override fun observeAckSyncStatus(alertId: String): Flow<com.sih26001.mobilealert.data.local.AckSyncStatus?> {
        return _pendingAckIds.map { ids ->
            if (ids.contains(alertId)) com.sih26001.mobilealert.data.local.AckSyncStatus.PENDING else null
        }
    }

    override suspend fun acknowledgeAlert(alertId: String): Result<Unit> {
        if (alertId.isBlank()) {
            return Result.failure(IllegalArgumentException("alertId cannot be blank"))
        }
        val trimmedId = alertId.trim()
        val currentList = _alerts.value
        val index = currentList.indexOfFirst { it.alertId == trimmedId }
        if (index == -1) {
            return Result.failure(NoSuchElementException("Alert $trimmedId not found"))
        }

        val existing = currentList[index]
        if (existing.status == AlertStatus.ACKNOWLEDGED) {
            return Result.success(Unit)
        }

        val now = Instant.now()
        val newStatus = if (existing.status == AlertStatus.EXPIRED) AlertStatus.EXPIRED else AlertStatus.ACKNOWLEDGED
        val updatedAlert = existing.copy(
            status = newStatus,
            acknowledgedAt = now
        )
        _alerts.value = currentList.toMutableList().apply { set(index, updatedAlert) }
        _pendingAckIds.value = _pendingAckIds.value + trimmedId
        return Result.success(Unit)
    }

    override suspend fun silenceAlert(alertId: String): Result<Unit> {
        if (alertId.isBlank()) {
            return Result.failure(IllegalArgumentException("alertId cannot be blank"))
        }
        val trimmedId = alertId.trim()
        val currentList = _alerts.value
        val index = currentList.indexOfFirst { it.alertId == trimmedId }
        if (index == -1) {
            return Result.failure(NoSuchElementException("Alert $trimmedId not found"))
        }

        val existing = currentList[index]
        if (existing.status == AlertStatus.ACKNOWLEDGED || existing.status == AlertStatus.EXPIRED) {
            return Result.success(Unit)
        }

        if (existing.status == AlertStatus.ACTIVE) {
            val updatedAlert = existing.copy(status = AlertStatus.SILENCED)
            _alerts.value = currentList.toMutableList().apply { set(index, updatedAlert) }
        }
        return Result.success(Unit)
    }

    override suspend fun refreshAlerts(): Result<Unit> {
        // Mock repository doesn't fetch from remote.
        return Result.success(Unit)
    }

    override suspend fun refreshAlert(alertId: String): Result<Alert> {
        val alert = _alerts.value.find { it.alertId == alertId }
        return if (alert != null) {
            Result.success(alert)
        } else {
            Result.failure(NoSuchElementException("Alert $alertId not found"))
        }
    }

    // --- Developer Controls ---

    /**
     * Triggers a test alert. Overwrites any existing alert with the same ID.
     */
    fun triggerTestAlert(alert: Alert) {
        val currentList = _alerts.value
        val existingIndex = currentList.indexOfFirst { it.alertId == alert.alertId }
        
        val newList = currentList.toMutableList()
        if (existingIndex != -1) {
            newList[existingIndex] = alert
        } else {
            newList.add(alert)
        }
        _alerts.value = newList
    }

    /**
     * Clears all alerts from memory.
     */
    fun clearAllAlerts() {
        _alerts.value = emptyList()
    }
}
