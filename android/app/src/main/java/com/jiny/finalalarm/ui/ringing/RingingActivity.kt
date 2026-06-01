package com.jiny.finalalarm.ui.ringing

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import com.jiny.finalalarm.ui.theme.FinalAlarmTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.content.ContextCompat
import com.jiny.finalalarm.core.alarm.AlarmForegroundService
import com.jiny.finalalarm.core.alarm.AlarmRingPayload
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RingingActivity : ComponentActivity() {

    private lateinit var payload: AlarmRingPayload
    private var approvalReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, null)
        }
        payload = AlarmRingPayload.fromBundle(intent.extras ?: Bundle())
        setContent {
            FinalAlarmTheme {
                val focusManager = LocalFocusManager.current
                val noInteraction = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = noInteraction,
                            indication = null,
                            onClick = { focusManager.clearFocus() },
                        )
                        .imePadding(),
                ) {
                    RingingRoot(payload, onFinished = { finishAndStopService() })
                }
            }
        }

        approvalReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                val eventId = i?.getStringExtra("eventId") ?: return
                if (eventId == payload.eventId) {
                    // VM이 폴링 또는 broadcast 수신해서 상태 갱신해도 되지만,
                    // 간단히 액티비티에 알리는 정도. UI는 다음 폴링 시 반영.
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            approvalReceiver,
            IntentFilter("com.jiny.finalalarm.UNLOCK_APPROVED"),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onDestroy() {
        approvalReceiver?.let { runCatching { unregisterReceiver(it) } }
        super.onDestroy()
    }

    private fun finishAndStopService() {
        startService(AlarmForegroundService.stopIntent(this))
        finish()
    }
}
