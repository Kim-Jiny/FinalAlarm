package com.jiny.finalalarm.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * AlarmManager가 지정 시각에 발화하는 BroadcastReceiver.
 * 즉시 AlarmForegroundService로 위임.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val payload = intent.extras ?: return
        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_START
            putExtras(payload)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        const val ACTION_FIRE = "com.jiny.finalalarm.action.FIRE_ALARM"
    }
}
