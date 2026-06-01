package app.finalalarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

const val CHANNEL_ALARM_FG = "alarm_fg"
const val CHANNEL_UNLOCK_REQUEST = "unlock_request"
const val CHANNEL_UNLOCK_APPROVED = "unlock_approved"
const val CHANNEL_SYSTEM = "system"

@HiltAndroidApp
class FinalAlarmApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM_FG,
                getString(R.string.channel_alarm_fg),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_UNLOCK_REQUEST,
                getString(R.string.channel_unlock_request),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_UNLOCK_APPROVED,
                getString(R.string.channel_unlock_approved),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SYSTEM,
                getString(R.string.channel_system),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}
