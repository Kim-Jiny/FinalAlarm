package com.jiny.finalalarm.core.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 알람 사운드 재생 + 진동.
 * - 코루틴 기반 볼륨 ramp
 * - stop() 시 모든 리소스 확실히 해제
 */
class AlarmAudioPlayer(private val ctx: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var rampJob: Job? = null

    fun start(payload: AlarmRingPayload) {
        stop()
        val uri = resolveSoundUri(payload.soundUri)
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(ctx, uri)
                isLooping = true
                prepare()
                setVolume(0f, 0f)
                start()
            }
            startVolumeRamp(payload.volume, payload.volumeRampSeconds)
        } catch (e: Exception) {
            Timber.e(e, "Audio player failed to start")
        }

        if (payload.vibrationEnabled) startVibration(payload.vibrationPattern)
    }

    fun stop() {
        rampJob?.cancel()
        rampJob = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun resolveSoundUri(s: String): Uri {
        if (s == "system:default" || s.isBlank()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        return Uri.parse(s)
    }

    private fun startVolumeRamp(targetPercent: Int, rampSeconds: Int) {
        val target = (targetPercent / 100f).coerceIn(0f, 1f)
        if (rampSeconds <= 0) {
            player?.setVolume(target, target)
            return
        }
        rampJob?.cancel()
        rampJob = scope.launch {
            val steps = 20
            val intervalMs = (rampSeconds * 1000L) / steps
            for (i in 1..steps) {
                if (!isActive) return@launch
                val v = target * (i.toFloat() / steps)
                player?.setVolume(v, v)
                delay(intervalMs)
            }
        }
    }

    private fun startVibration(pattern: String) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val timings = when (pattern) {
            "SHORT" -> longArrayOf(0, 200, 800)
            "MEDIUM" -> longArrayOf(0, 500, 1000)
            "LONG" -> longArrayOf(0, 1500, 1000)
            "PULSE" -> longArrayOf(0, 300, 200, 300, 200, 300, 1000)
            "HEARTBEAT" -> longArrayOf(0, 100, 100, 300, 800)
            else -> longArrayOf(0, 500, 500)
        }
        val effect = VibrationEffect.createWaveform(timings, 0)
        vibrator?.vibrate(effect)
    }
}
