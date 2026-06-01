package app.finalalarm.core.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.finalalarm.CHANNEL_UNLOCK_APPROVED
import app.finalalarm.CHANNEL_UNLOCK_REQUEST
import app.finalalarm.MainActivity
import app.finalalarm.R
import app.finalalarm.core.alarm.AlarmForegroundService
import app.finalalarm.core.alarm.AlarmRingPayload
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FinalAlarmMessagingService : FirebaseMessagingService() {

    @Inject lateinit var tokenRegistrar: PushTokenRegistrar

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        Timber.d("FCM data: %s", data)
        when (data["type"]) {
            "ALARM_RING" -> handleAlarmRing(data)
            "UNLOCK_REQUEST" -> showUnlockRequestNotif(data)
            "UNLOCK_APPROVED" -> showUnlockApprovedNotif(data)
            else -> Timber.w("Unknown FCM type: %s", data["type"])
        }
    }

    override fun onNewToken(token: String) {
        Timber.d("New FCM token")
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { tokenRegistrar.register(token) }
        }
    }

    private fun handleAlarmRing(data: Map<String, String>) {
        val payload = AlarmRingPayload.fromDataMap(data)
        val intent = Intent(this, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_START
            putExtras(payload.toBundle())
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun showUnlockRequestNotif(data: Map<String, String>) {
        val requesterName = data["requesterDisplayName"].orEmpty()
        val requestId = data["requestId"].orEmpty()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("deepLink", "inbox/request/$requestId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, requestId.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_UNLOCK_REQUEST)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$requesterName 님이 잠금해제를 요청했어요")
            .setContentText("탭하여 승인")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(requestId.hashCode(), notif)
    }

    private fun showUnlockApprovedNotif(data: Map<String, String>) {
        val approver = data["approvedByDisplayName"].orEmpty()
        val notif = NotificationCompat.Builder(this, CHANNEL_UNLOCK_APPROVED)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$approver 님이 승인했어요")
            .setContentText("이제 알람을 끌 수 있어요")
            .setAutoCancel(true)
            .build()
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(data["requestId"]?.hashCode() ?: 0, notif)

        // RingingActivity에 상태 갱신 브로드캐스트
        val intent = Intent("app.finalalarm.UNLOCK_APPROVED").apply {
            setPackage(packageName)
            putExtra("eventId", data["eventId"])
            putExtra("requestId", data["requestId"])
        }
        sendBroadcast(intent)
    }
}
