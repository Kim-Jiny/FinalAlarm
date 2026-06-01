package com.jiny.finalalarm.core.device

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager

/**
 * 알람 발사·해제 시점의 디바이스 상태 스냅샷.
 * - volumePct: 알람 스트림 볼륨 백분율 (0~100). 0이면 사용자가 알람을 무음으로 내려놓은 것.
 * - dnd: 방해금지 모드 켜져 있는지. 기본적으로 알람은 통과하지만 사용자가 "알람도 차단" 설정한 경우가 있음.
 */
data class DeviceState(val volumePct: Int, val dnd: Boolean) {
    companion object {
        fun probe(ctx: Context): DeviceState {
            val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val cur = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
            val pct = ((cur.toFloat() / max) * 100).toInt().coerceIn(0, 100)

            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val dnd = try {
                nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                    nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            } catch (_: SecurityException) { false }

            return DeviceState(pct, dnd)
        }
    }
}
