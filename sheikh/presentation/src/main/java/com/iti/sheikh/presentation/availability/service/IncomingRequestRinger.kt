package com.iti.sheikh.presentation.availability.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Loops the device's default ringtone + a vibration pattern while an incoming meeting request is
 * pending, respecting ringer mode/DND (never bypasses silent mode). Owns a hard [RING_TIMEOUT_MS]
 * cap as a safety net in case [SheikhAvailabilityForegroundService]'s state-driven [stop] call is
 * ever missed for an unrelated reason.
 */
class IncomingRequestRinger(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timeoutJob: Job? = null

    fun start() {
        stop()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        if (ringerMode != AudioManager.RINGER_MODE_SILENT) vibrate()
        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) playRingtone()

        timeoutJob = scope.launch {
            delay(RING_TIMEOUT_MS)
            stop()
        }
    }

    fun stop() {
        timeoutJob?.cancel()
        timeoutJob = null
        mediaPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            player.release()
        }
        mediaPlayer = null
        stopVibration()
    }

    private fun playRingtone() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE) ?: return
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure { Log.w(TAG, "playRingtone: failed to start ringtone", it) }
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 800, 600)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager ?: return
                vibratorManager.vibrate(CombinedVibration.createParallel(VibrationEffect.createWaveform(pattern, 1)))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 1)
            }
        }.onFailure { Log.w(TAG, "vibrate: failed to start vibration", it) }
    }

    private fun stopVibration() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.cancel()
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
            }
        }
    }

    private companion object {
        const val TAG = "IncomingRequestRinger"
        const val RING_TIMEOUT_MS = 45_000L
    }
}
