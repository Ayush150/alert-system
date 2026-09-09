package com.sih26001.mobilealert.data.ack

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates ACK synchronization based on Android system connectivity events.
 *
 * It prevents duplicate concurrent jobs when multiple networks become available simultaneously.
 */
class AckSyncCoordinator(
    context: Context,
    private val engine: AckSyncEngine,
    private val scope: CoroutineScope
) {
    private val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val jobMutex = Mutex()
    private var currentJob: Job? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("AckSyncCoordinator", "Network available. Triggering recovery and reconciliation.")
            triggerSync()
        }
    }

    /**
     * Registers the network callback to observe connectivity changes.
     */
    fun startObserving() {
        if (connectivityManager != null) {
            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
                Log.d("AckSyncCoordinator", "Started observing network connectivity.")
            } catch (e: Exception) {
                Log.e("AckSyncCoordinator", "Failed to register network callback", e)
            }
        }
    }

    /**
     * Unregisters the network callback.
     */
    fun stopObserving() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
            Log.d("AckSyncCoordinator", "Stopped observing network connectivity.")
        } catch (e: Exception) {
            Log.e("AckSyncCoordinator", "Failed to unregister network callback", e)
        }
    }

    /**
     * Safely triggers the recovery and reconciliation pipeline on the application scope.
     * Guaranteed to prevent creating a massive backlog of concurrent jobs.
     */
    fun triggerSync() {
        scope.launch {
            jobMutex.withLock {
                // If a job is already running, let it finish.
                // We don't necessarily cancel it; we just avoid spawning a new one.
                if (currentJob?.isActive == true) {
                    Log.d("AckSyncCoordinator", "Sync job already running. Skipping duplicate trigger.")
                    return@launch
                }

                currentJob = scope.launch {
                    try {
                        val recovered = engine.recoverInterruptedSyncs()
                        if (recovered > 0) {
                            Log.i("AckSyncCoordinator", "Recovered $recovered stale IN_FLIGHT records.")
                        }
                        
                        val processed = engine.reconcilePendingAcks()
                        if (processed > 0) {
                            Log.i("AckSyncCoordinator", "Reconciled $processed ACK records.")
                        }
                    } catch (t: Throwable) {
                        Log.e("AckSyncCoordinator", "Unexpected error during coordinated sync", t)
                    }
                }
            }
        }
    }
}
