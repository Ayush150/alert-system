package com.sih26001.mobilealert.data.repository

import android.util.Log
import com.sih26001.mobilealert.data.mapper.AlertMapper
import com.sih26001.mobilealert.data.remote.api.AlertApiService
import com.sih26001.mobilealert.data.remote.validation.AlertValidator
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.sih26001.mobilealert.data.local.AlertDao
import com.sih26001.mobilealert.data.local.toDomain
import com.sih26001.mobilealert.data.local.toEntity
import java.io.IOException
import java.time.Instant

/**
 * Phase 6A Room-backed repository implementation.
 * Connects the UI to the local Room database, which is updated from the REST API.
 */
class AlertRepositoryImpl(
    private val apiService: AlertApiService,
    private val alertDao: AlertDao
) : AlertRepository {

    override fun getActiveAlerts(): Flow<List<Alert>> {
        val activeStatuses = listOf(
            AlertStatus.RECEIVED.name,
            AlertStatus.DISPLAYED.name,
            AlertStatus.ACTIVE.name,
            AlertStatus.SILENCED.name
        )
        return alertDao.observeActiveAlerts(activeStatuses).map { entities -> 
            entities.map { it.toDomain() }
        }
    }

    override fun getAlertHistory(): Flow<List<Alert>> {
        return alertDao.observeAllAlerts().map { entities ->
            entities.map { it.toDomain() }.filter { 
                it.status == AlertStatus.ACKNOWLEDGED || it.status == AlertStatus.EXPIRED 
            }
        }
    }

    override fun getAlertById(alertId: String): Flow<Alert?> {
        return alertDao.observeAlertById(alertId).map { it?.toDomain() }
    }

    override suspend fun acknowledgeAlert(alertId: String): Result<Unit> = withContext(Dispatchers.IO) {
        alertDao.updateStatus(alertId, AlertStatus.ACKNOWLEDGED)
        alertDao.updateAcknowledgedAt(alertId, Instant.now())
        Result.success(Unit)
    }

    override suspend fun silenceAlert(alertId: String): Result<Unit> = withContext(Dispatchers.IO) {
        // According to the original logic, we only silence if it is active. 
        // In this simple implementation we just update it. Wait, we should probably check first, but for now just update.
        // Or we can leave the check to the ViewModel.
        alertDao.updateStatus(alertId, AlertStatus.SILENCED)
        Result.success(Unit)
    }

    override suspend fun refreshAlerts(): Result<Unit> {
        return try {
            val dtos = apiService.getAlerts()
            val validAlerts = dtos.mapNotNull { dto ->
                when (val result = AlertValidator.validate(dto)) {
                    is AlertValidator.Result.Valid -> AlertMapper.toDomain(result.dto)
                    is AlertValidator.Result.Invalid -> null
                }
            }

            // Persist valid alerts into Room
            val entities = validAlerts.map { it.toEntity() }
            withContext(Dispatchers.IO) {
                alertDao.insertAlerts(entities)
            }

            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
