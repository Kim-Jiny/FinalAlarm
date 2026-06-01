package app.finalalarm.ui.ringing

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import app.finalalarm.core.alarm.AlarmForegroundService
import app.finalalarm.core.alarm.AlarmRingPayload
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RingingActivity : ComponentActivity() {

    private lateinit var payload: AlarmRingPayload
    private var approvalReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, null)
        }
        payload = AlarmRingPayload.fromBundle(intent.extras ?: Bundle())
        setContent {
            MaterialTheme {
                Surface { RingingRoot(payload, onFinished = { finishAndStopService() }) }
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
            IntentFilter("app.finalalarm.UNLOCK_APPROVED"),
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
