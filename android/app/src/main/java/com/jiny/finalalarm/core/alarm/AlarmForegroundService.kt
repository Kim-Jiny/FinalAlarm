package com.jiny.finalalarm.core.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.jiny.finalalarm.CHANNEL_ALARM_FG
import com.jiny.finalalarm.R
import com.jiny.finalalarm.core.sync.EventReconcileWorker
import com.jiny.finalalarm.core.sync.PendingEventStore
import com.jiny.finalalarm.data.api.CreateEventReq
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.ui.ringing.RingingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class AlarmForegroundService : Service() {

    @Inject lateinit var api: FinalAlarmApi
    @Inject lateinit var pendingStore: PendingEventStore

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
        val defId = payload.definitionId
            ?: return payload.copy(eventId = "local-${UUID.randomUUID()}")

        val triggeredAt = OffsetDateTime.now().toString()
        val ds = com.jiny.finalalarm.core.device.DeviceState.probe(applicationContext)
        val delays = listOf(0L, 1_000L, 2_000L, 4_000L, 8_000L) // 총 ~15s 동안 5회
        for ((i, d) in delays.withIndex()) {
            if (d > 0) delay(d)
            val result = runCatching {
                api.createEvent(
                    CreateEventReq(
                        definitionId = defId,
                        triggeredAt = triggeredAt,
                        volumePctAtTrigger = ds.volumePct,
                        dndAtTrigger = ds.dnd,
                    ),
                )
            }
            result.getOrNull()?.let { event ->
                return payload.copy(eventId = event.id)
            }
            Timber.w(result.exceptionOrNull(), "createEvent attempt ${i + 1} failed")
        }

        // 끝내 실패 → 로컬 UUID + pending queue에 적재 (네트워크 복구 시 reconcile)
        val localId = "local-${UUID.randomUUID()}"
        runCatching {
            pendingStore.add(
                PendingEventStore.Pending(
                    localId = localId,
                    definitionId = defId,
                    triggeredAt = triggeredAt,
                    volumePctAtTrigger = ds.volumePct,
                    dndAtTrigger = ds.dnd,
                ),
            )
            EventReconcileWorker.enqueue(applicationContext)
        }
        return payload.copy(eventId = localId)
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
        player?.release()
        player = null
        releaseWakeLock()
        val nm = getSystemService(NotificationManager::class.java)
        nm?.cancel(NOTIF_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseWakeLock() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        Timber.d("AlarmForegroundService destroying")
        player?.release()
        player = null
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.jiny.finalalarm.action.SERVICE_START"
        const val ACTION_STOP = "com.jiny.finalalarm.action.SERVICE_STOP"
        private const val NOTIF_ID = 1001

        fun stopIntent(ctx: Context) = Intent(ctx, AlarmForegroundService::class.java).apply {
            action = ACTION_STOP
        }
    }
}
