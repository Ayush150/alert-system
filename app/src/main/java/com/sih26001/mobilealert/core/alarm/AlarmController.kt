package com.sih26001.mobilealert.core.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity

interface AlarmController {
    fun startAlarm(alert: Alert)
    fun silenceAlarm(alertId: String)
    fun stopAlarm()
}

class AlarmControllerImpl(private val context: Context) : AlarmController {

    private var mediaPlayer: MediaPlayer? = null
    private var activeAlertId: String? = null
    
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Starts the alarm (sound and vibration) for a given alert based on its severity.
     */
    override fun startAlarm(alert: Alert) {
        if (alert.severity == AlertSeverity.NORMAL) {
            return // Normal alerts do not trigger emergency alarms
        }

        // Avoid restarting if the same alert is already playing
        if (activeAlertId == alert.alertId && isPlaying()) {
            return
        }

        stopAlarm() // Stop any currently playing alarm

        activeAlertId = alert.alertId

        startVibration(alert.severity)
        
        if (alert.severity == AlertSeverity.CRITICAL) {
            startSound()
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
        } catch (e: Exception) {
            e.printStackTrace()
            // In a production app we would log this properly without crashing.
        }
    }

    /**
     * Silences the alarm for a specific alert ID.
     */
    override fun silenceAlarm(alertId: String) {
        if (activeAlertId == alertId) {
            stopAlarm()
        }
    }

    /**
     * Stops the alarm entirely and cleans up resources.
     */
    override fun stopAlarm() {
        activeAlertId = null
        
        vibrator.cancel()
        
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    private fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
}
