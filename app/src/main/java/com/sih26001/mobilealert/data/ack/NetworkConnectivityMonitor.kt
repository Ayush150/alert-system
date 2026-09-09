package com.sih26001.mobilealert.data.ack

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Abstraction for inspecting device network connectivity.
 * Allows the synchronization engine to avoid coupling directly to Android's ConnectivityManager.
 */
interface NetworkConnectivityMonitor {
    /**
     * Returns true if the device currently possesses a validated internet capability.
     * Note: Having network connectivity does not guarantee the backend is reachable or healthy.
     */
    fun isOnline(): Boolean
}

/**
 * Android implementation of [NetworkConnectivityMonitor] using [ConnectivityManager].
 */
class AndroidNetworkConnectivityMonitor(
    context: Context
) : NetworkConnectivityMonitor {

    private val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override fun isOnline(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
