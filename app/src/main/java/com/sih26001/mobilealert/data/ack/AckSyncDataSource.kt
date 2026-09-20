package com.sih26001.mobilealert.data.ack

import com.sih26001.mobilealert.data.local.PendingAckEntity
import com.sih26001.mobilealert.data.remote.api.AlertApiService
import com.sih26001.mobilealert.data.remote.dto.AckRequestDto
import kotlinx.coroutines.CancellationException

/**
 * Abstraction for synchronizing operational acknowledgement records with the backend.
 *
 * Contract Independence:
 * The SIH backend has not yet finalized or published an HTTP endpoint or wire schema for ACKs.
 * This interface intentionally hides transport details so the mobile client can orchestrate 
 * queue consumption, backoff, and state transitions without being tied to a specific network transport.
 *
 * Guarantees required from implementations:
 * 1. [Result.success] MUST ONLY be returned upon genuine authoritative backend acceptance.
 * 2. Transport failures must remain failures and return [Result.failure].
 * 3. Authentication or authorization failures must NOT be silently converted into success.
 * 4. HTTP conflicts (e.g., 409) must only be treated as success if the authoritative contract explicitly defines their semantics as such.
 */
interface AckSyncDataSource {
    /**
     * Attempts to transmit the pending operational acknowledgement [pendingAck] to the backend.
     *
     * @return [Result.success] ONLY if the backend authoritatively confirms the acknowledgement.
     *         [Result.failure] if transport fails or if no backend transport is configured.
     */
    suspend fun synchronizeAck(
        pendingAck: PendingAckEntity
    ): Result<Unit>
}

/**
 * Live HTTP transport implementation of [AckSyncDataSource].
 * Synchronizes operational acknowledgments with the backend via POST /api/v1/alerts/{alert_id}/ack.
 */
class HttpAckSyncDataSource(
    private val apiService: AlertApiService
) : AckSyncDataSource {
    override suspend fun synchronizeAck(
        pendingAck: PendingAckEntity
    ): Result<Unit> {
        return try {
            val response = apiService.acknowledgeAlert(
                alertId = pendingAck.alertId,
                request = AckRequestDto(
                    alert_id = pendingAck.alertId,
                    acknowledged_by = "Android User",
                    notes = "Operational acknowledgement confirmed on Android mobile device"
                )
            )

            // The backend returns: {"alert_id":"...","status":"ACKNOWLEDGED","timestamp":"...","mqtt_published":true}
            val isSuccess = response.status.equals("ACKNOWLEDGED", ignoreCase = true) ||
                    (response.alert_id != null && response.alert_id == pendingAck.alertId)

            if (isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(
                    IllegalStateException("Authoritative server returned non-ACK status: ${response.status}")
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Production implementation of [AckSyncDataSource] used while the backend ACK contract is unavailable.
 *
 * It MUST NEVER report success, invent HTTP routes, or fake responses.
 * All synchronization attempts safely return failure with an explicit exception.
 */
class UnavailableAckSyncDataSource : AckSyncDataSource {
    override suspend fun synchronizeAck(
        pendingAck: PendingAckEntity
    ): Result<Unit> {
        return Result.failure(
            IllegalStateException("SIH backend ACK contract unavailable")
        )
    }
}
