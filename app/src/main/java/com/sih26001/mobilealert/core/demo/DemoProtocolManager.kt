package com.sih26001.mobilealert.core.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deterministic states for the SIH26001 Demo Protocol emergency simulation lifecycle.
 */
enum class DemoProtocolState {
    INACTIVE,
    NORMAL,
    WARNING,
    HIGH_ALERT,
    RESOLVED
}

/**
 * Manages the lifecycle state machine of the SIH26001 Demo Protocol.
 *
 * ARCHITECTURAL CONTRACT:
 * 1. The Demo Protocol is controlled EXCLUSIVELY from the SIXTH SENSE web command center.
 * 2. The Android application is a receiver-only end-user client.
 * 3. Local state transitions on the Android client are permanently disabled.
 * 4. State is permanently INACTIVE on the Android device; physical alarm gating is governed
 *    directly by authoritative backend-delivered alerts via FCM.
 */
interface DemoProtocolManager {
    val isStarted: StateFlow<Boolean>
    val state: StateFlow<DemoProtocolState>

    fun startProtocol()
    fun stopProtocol()
    fun setNormal()
    fun setWarning()
    fun setHighAlert()
    fun setResolved()
}

class DemoProtocolManagerImpl : DemoProtocolManager {
    private val _state = MutableStateFlow(DemoProtocolState.INACTIVE)
    override val state: StateFlow<DemoProtocolState> = _state.asStateFlow()

    private val _isStarted = MutableStateFlow(false)
    override val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    override fun startProtocol() {
        // Disabled: Android client does not control or mutate Demo Protocol state
    }

    override fun stopProtocol() {
        // Disabled: Android client does not control or mutate Demo Protocol state
    }

    override fun setNormal() {
        // Disabled: Android client does not control or mutate Demo Protocol state
    }

    override fun setWarning() {
        // Disabled: Android client does not control or mutate Demo Protocol state
    }

    override fun setHighAlert() {
        // Disabled: Android client does not control or mutate Demo Protocol state
    }

    override fun setResolved() {
        // Disabled: Android client does not control or mutate Demo Protocol state
    }
}
