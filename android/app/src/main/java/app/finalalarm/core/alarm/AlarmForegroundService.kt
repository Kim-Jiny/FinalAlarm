package app.finalalarm.core.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import app.finalalarm.CHANNEL_ALARM_FG
import app.finalalarm.R
import app.finalalarm.data.api.CreateEventReq
import app.finalalarm.data.api.FinalAlarmApi
import app.finalalarm.ui.ringing.RingingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class AlarmForegroundService : Service() {

    @Inject lateinit var api: FinalAlarmApi

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var player: AlarmAudioPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> stopAndFinish()
            else -> stopAndFinish()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val initial = AlarmRingPayload.fromBundle(intent.extras ?: return)
        // ANR 방지: 5초 안에 startForeground 호출 필요. payload 이벤트ID 보강은 비동기로.
        startForeground(NOTIF_ID, buildNotification(initial))
        acquireWakeLock()

        player?.stop()
        player = AlarmAudioPlayer(this).also { it.start(initial) }

        if (initial.eventId.isNotBlank()) {
            // FCM 경로 — 이미 서버 eventId 있음
            launchRingingActivity(initial)
        } else {
            // 로컬 발사 → 서버에 보고하고 진짜 eventId 받아오기
            scope.launch {
                val resolved = ensureServerEventId(initial)
                launchRingingActivity(resolved)
            }
        }
    }

    private suspend fun ensureServerEventId(payload: AlarmRingPayload): AlarmRingPayload {
        val defId = payload.definitionId ?: return payload.copy(eventId = "local-${UUID.randomUUID()}")
        return try {
            val event = api.createEvent(CreateEventReq(definitionId = defId))
            payload.copy(eventId = event.id)
        } catch (e: Exception) {
            // 오프라인/서버 다운: 로컬 UUID로 진행. PERSONAL이면 클라 단에서만 dismiss 가능.
            Timber.w(e, "createEvent failed; using local UUID")
            payload.copy(eventId = "local-${UUID.randomUUID()}")
        }
    }

    private fun launchRingingActivity(payload: AlarmRingPayload) {
        val ringingIntent = Intent(this, RingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtras(payload.toBundle())
        }
        startActivity(ringingIntent)
    }

    private fun buildNotification(payload: AlarmRingPayload) =
        NotificationCompat.Builder(this, CHANNEL_ALARM_FG)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("알람 울리는 중")
            .setContentText(payload.label.ifBlank { "알람" })
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setFullScreenIntent(buildFullScreenPi(payload), true)
            .build()

    private fun buildFullScreenPi(payload: AlarmRingPayload): PendingIntent {
        val i = Intent(this, RingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtras(payload.toBundle())
        }
        return PendingIntent.getActivity(
            this, payload.eventId.hashCode().takeIf { it != 0 } ?: payload.definitionId.hashCode(), i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "FinalAlarm:RingWakeLock",
        ).apply { setReferenceCounted(false); acquire(15 * 60 * 1000L) }
    }

    private fun stopAndFinish() {
        player?.stop()
        player = null
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
        val nm = getSystemService(NotificationManager::class.java)
        nm?.cancel(NOTIF_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Timber.d("AlarmForegroundService destroying")
        player?.stop()
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "app.finalalarm.action.SERVICE_START"
        const val ACTION_STOP = "app.finalalarm.action.SERVICE_STOP"
        private const val NOTIF_ID = 1001

        fun stopIntent(ctx: Context) = Intent(ctx, AlarmForegroundService::class.java).apply {
            action = ACTION_STOP
        }
    }
}
