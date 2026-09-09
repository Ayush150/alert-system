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

    override suspend fun acknowledgeAlert(alertId: String): Result<Unit> {
        val currentList = _alerts.value
        val index = currentList.indexOfFirst { it.alertId == alertId }
        if (index != -1) {
            val updatedAlert = currentList[index].copy(
                status = AlertStatus.ACKNOWLEDGED,
                acknowledgedAt = Instant.now()
            )
            _alerts.value = currentList.toMutableList().apply { set(index, updatedAlert) }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Alert not found"))
    }

    override suspend fun silenceAlert(alertId: String): Result<Unit> {
        val currentList = _alerts.value
        val index = currentList.indexOfFirst { it.alertId == alertId }
        if (index != -1) {
            // Only active alerts can be silenced.
            if (currentList[index].status == AlertStatus.ACTIVE) {
                val updatedAlert = currentList[index].copy(status = AlertStatus.SILENCED)
                _alerts.value = currentList.toMutableList().apply { set(index, updatedAlert) }
            }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Alert not found"))
    }

    override suspend fun refreshAlerts(): Result<Unit> {
        // Mock repository doesn't fetch from remote.
        return Result.success(Unit)
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
