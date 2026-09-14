package com.sih26001.mobilealert.data.ack

import com.sih26001.mobilealert.data.local.PendingAckEntity

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
