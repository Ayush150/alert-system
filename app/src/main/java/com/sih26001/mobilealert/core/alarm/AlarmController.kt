package com.sih26001.mobilealert.core.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AlarmController {
    val isSirenActive: StateFlow<Boolean>
    fun startAlarm(alert: Alert)
    fun silenceAlarm(alertId: String)
    fun stopAlarm()
}

class AlarmControllerImpl(private val context: Context) : AlarmController {

    companion object {
        private const val TAG = "AlarmController"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var activeAlertId: String? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _isSirenActive = MutableStateFlow(false)
    override val isSirenActive: StateFlow<Boolean> = _isSirenActive.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Starts the alarm (sound and vibration) for a given emergency alert based on its severity.
     * Guaranteed idempotent: repeat calls for the same active alert do not restart siren.
     */
    override fun startAlarm(alert: Alert) {
        if (alert.severity == AlertSeverity.NORMAL) {
            return // Normal alerts do not trigger emergency alarms
        }

        // Avoid restarting if the same alert is already playing
        if (activeAlertId == alert.alertId && isPlaying()) {
            Log.d(TAG, "Alarm already active for alert_id=${alert.alertId}; skipping duplicate start")
            return
        }

        stopAlarm() // Cleanly stop any existing alarm before starting anew

        activeAlertId = alert.alertId
        _isSirenActive.value = true

        startVibration(alert.severity)

        if (alert.severity in setOf(AlertSeverity.HIGH, AlertSeverity.CRITICAL)) {
            requestAudioFocus()
            startSound()
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* Handle focus changes if needed */ }
                    .build()
                audioFocusRequest = focusReq
                audioManager.requestAudioFocus(focusReq)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request audio focus: ${e.message}")
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to abandon audio focus: ${e.message}")
        }
    }

    private fun startVibration(severity: AlertSeverity) {
        if (!vibrator.hasVibrator()) return

        val pattern = when (severity) {
            AlertSeverity.CRITICAL -> longArrayOf(0, 500, 200, 500, 200) // Aggressive
            AlertSeverity.HIGH -> longArrayOf(0, 1000, 1000) // Standard slow pulse
            else -> return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 1)) // 1 means repeat from index 1
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 1)
        }
    }

    private fun startSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.i(TAG, "Emergency siren sound started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start emergency siren sound: ${e.message}", e)
        }
    }

    /**
     * Silences the alarm for a specific alert ID.
     */
    override fun silenceAlarm(alertId: String) {
        if (activeAlertId == alertId || activeAlertId == null) {
            stopAlarm()
        }
    }

    /**
     * Stops the alarm entirely, cleans up resources, abandons audio focus.
     */
    override fun stopAlarm() {
        activeAlertId = null
        _isSirenActive.value = false

        try {
            vibrator.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling vibrator: ${e.message}")
        }

        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing media player: ${e.message}")
            }
        }
        mediaPlayer = null

        abandonAudioFocus()
    }

    private fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }
}
