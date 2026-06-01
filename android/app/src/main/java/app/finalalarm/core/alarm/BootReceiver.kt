package app.finalalarm.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.finalalarm.core.work.AlarmRescheduleWorker

/**
 * 재부팅·앱 업데이트 후 알람을 다시 등록.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<AlarmRescheduleWorker>().build(),
                )
            }
        }
    }
}
