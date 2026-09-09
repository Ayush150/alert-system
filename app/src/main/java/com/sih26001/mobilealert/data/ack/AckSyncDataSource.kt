package com.sih26001.mobilealert.data.ack

import com.sih26001.mobilealert.data.local.PendingAckEntity

/**
 * Abstraction for synchronizing operational acknowledgement records with the backend.
 *
 * Contract Independence:
 * The SIH backend has not yet finalized or published an HTTP endpoint or wire schema for ACKs.
 * This abstraction allows the mobile client to orchestrate queue consumption, backoff,
 * and state transitions without being tied to any specific network transport.
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
