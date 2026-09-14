package com.sih26001.mobilealert.data.ack

import android.util.Log
import java.time.Instant

/**
 * Interface for decoupled, deterministic observability of the ACK state machine.
 */
interface AckSyncEventLogger {
    fun recordCreated(alertId: String)
    fun recordQueued(alertId: String)
    fun recordAttempt(alertId: String, retryCount: Int)
    fun recordFailure(alertId: String, retryCount: Int, message: String?)
    fun recordRecovered(alertId: String)
    fun recordCompleted(alertId: String, timestamp: Instant)
}

/**
 * Runtime logging implementation of [AckSyncEventLogger].
 */
class AndroidAckSyncEventLogger : AckSyncEventLogger {
    companion object {
        private const val TAG = "AckSyncLifecycle"
    }

    override fun recordCreated(alertId: String) {
        Log.i(TAG, "ACK_CREATED: alertId=$alertId")
    }

    override fun recordQueued(alertId: String) {
        Log.i(TAG, "ACK_QUEUED: alertId=$alertId")
    }

    override fun recordAttempt(alertId: String, retryCount: Int) {
        Log.i(TAG, "ACK_SYNC_ATTEMPT: alertId=$alertId, retryCount=$retryCount")
    }

    override fun recordFailure(alertId: String, retryCount: Int, message: String?) {
        Log.w(TAG, "ACK_SYNC_FAILED: alertId=$alertId, retryCount=$retryCount, reason=$message")
    }

    override fun recordRecovered(alertId: String) {
        Log.i(TAG, "ACK_RECOVERED: alertId=$alertId transitioned to FAILED from stale IN_FLIGHT")
    }

    override fun recordCompleted(alertId: String, timestamp: Instant) {
        Log.i(TAG, "ACK_SYNC_COMPLETED: alertId=$alertId officially accepted by backend at $timestamp")
    }
}
