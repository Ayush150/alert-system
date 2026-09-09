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

import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.PendingAckDao
import com.sih26001.mobilealert.data.local.PendingAckEntity

/**
 * Phase 6A/6D Room-backed repository implementation.
 * Connects the UI to the local Room database, which is updated from the REST API.
 */
class AlertRepositoryImpl(
    private val apiService: AlertApiService,
    private val alertDao: AlertDao,
    private val pendingAckDao: PendingAckDao,
    private val transactionRunner: com.sih26001.mobilealert.data.local.DatabaseTransactionRunner = object : com.sih26001.mobilealert.data.local.DatabaseTransactionRunner {
        override suspend fun <T> invoke(block: suspend () -> T): T = block()
    },
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
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

    override fun observePendingAckIds(): Flow<Set<String>> {
        return pendingAckDao.observePendingAcks().map { list ->
            list.map { it.alertId }.toSet()
        }
    }

    override fun observeAckSyncStatus(alertId: String): Flow<AckSyncStatus?> {
        return pendingAckDao.observeAckSyncStatus(alertId)
    }

    override suspend fun acknowledgeAlert(alertId: String): Result<Unit> = withContext(ioDispatcher) {
        if (alertId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("alertId cannot be blank"))
        }

        val trimmedId = alertId.trim()
        val alertEntity = alertDao.getAlertById(trimmedId)
            ?: return@withContext Result.failure(NoSuchElementException("Alert $trimmedId not found"))

        // Idempotency: if already acknowledged, return success without duplicate actions
        if (alertEntity.status == AlertStatus.ACKNOWLEDGED) {
            return@withContext Result.success(Unit)
        }

        val now = Instant.now()
        // If expired, preserve EXPIRED status while updating acknowledgedAt
        val newStatus = if (alertEntity.status == AlertStatus.EXPIRED) {
            AlertStatus.EXPIRED
        } else {
            AlertStatus.ACKNOWLEDGED
        }

        // Room transaction ensures AlertEntity status update and PendingAckEntity insertion are atomic
        try {
            transactionRunner {
                alertDao.updateStatus(trimmedId, newStatus)
                alertDao.updateAcknowledgedAt(trimmedId, now)

                // Enqueue into Room offline queue for future authoritative backend synchronization
                val pendingAck = PendingAckEntity(
                    alertId = trimmedId,
                    acknowledgedAt = now,
                    retryCount = 0,
                    lastAttemptAt = null,
                    status = AckSyncStatus.PENDING
                )
                pendingAckDao.insertOrIgnore(pendingAck)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun silenceAlert(alertId: String): Result<Unit> = withContext(ioDispatcher) {
        if (alertId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("alertId cannot be blank"))
        }

        val trimmedId = alertId.trim()
        val alertEntity = alertDao.getAlertById(trimmedId)
            ?: return@withContext Result.failure(NoSuchElementException("Alert $trimmedId not found"))

        // Do not alter status if already acknowledged or expired
        if (alertEntity.status == AlertStatus.ACKNOWLEDGED || alertEntity.status == AlertStatus.EXPIRED) {
            return@withContext Result.success(Unit)
        }

        // SILENCE transitions ACTIVE -> SILENCED only
        if (alertEntity.status == AlertStatus.ACTIVE) {
            alertDao.updateStatus(trimmedId, AlertStatus.SILENCED)
        }

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
            withContext(ioDispatcher) {
                alertDao.insertAlerts(entities)
            }

            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshAlert(alertId: String): Result<Alert> {
        return try {
            val dtos = apiService.getAlerts()
            val matchingDto = dtos.find { it.alert_id == alertId }
                ?: return Result.failure(NoSuchElementException("Alert $alertId not found in remote alerts"))

            val validationResult = AlertValidator.validate(matchingDto)
            if (validationResult is AlertValidator.Result.Invalid) {
                return Result.failure(IllegalArgumentException("Malformed alert payload for $alertId: ${validationResult.reasons}"))
            }

            val validDto = (validationResult as AlertValidator.Result.Valid).dto
            var domainAlert = AlertMapper.toDomain(validDto)

            // Check if the alert has expired
            val now = Instant.now()
            if (domainAlert.expiresAt != null && domainAlert.expiresAt.isBefore(now)) {
                domainAlert = domainAlert.copy(status = AlertStatus.EXPIRED)
            }

            // Persist valid alert into Room
            val entity = domainAlert.toEntity()
            withContext(ioDispatcher) {
                alertDao.insertAlert(entity)
            }

            Result.success(domainAlert)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
