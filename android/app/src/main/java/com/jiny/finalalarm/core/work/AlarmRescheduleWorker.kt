package com.jiny.finalalarm.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jiny.finalalarm.core.alarm.AlarmRingPayload
import com.jiny.finalalarm.core.alarm.AlarmScheduler
import com.jiny.finalalarm.core.alarm.ScheduledAlarmStore
import com.jiny.finalalarm.core.auth.TokenStore
import com.jiny.finalalarm.data.api.FinalAlarmApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import timber.log.Timber

@HiltWorker
class AlarmRescheduleWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val api: FinalAlarmApi,
    private val scheduler: AlarmScheduler,
    private val scheduledStore: ScheduledAlarmStore,
    private val tokenStore: TokenStore,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        // 로그인 안 된 상태에서는 그냥 스킵 (로그인 후 다시 enqueue됨)
        if (tokenStore.access() == null) {
            Timber.d("AlarmRescheduleWorker: no token, skipping")
            return Result.success()
        }

        return runCatching {
            val alarms = api.listAlarms(active = true)
            val activeIds = alarms.map { it.id }.toSet()

            val previouslyScheduled = scheduledStore.list()
            (previouslyScheduled - activeIds).forEach { scheduler.cancel(it) }

            val missionsById = runCatching { api.listMissions() }.getOrDefault(emptyList())
                .associateBy { it.id }

            alarms.forEach { a ->
                val mission = missionsById[a.missionId]
                val payload = AlarmRingPayload(
                    eventId = "",
                    definitionId = a.id,
                    alarmKind = a.kind.name,
                    senderUserId = null,
                    senderDisplayName = null,
                    label = a.label,
                    missionId = a.missionId,
                    missionType = mission?.type?.name.orEmpty(),
                    missionConfigJson = mission?.config?.let {
                        kotlinx.serialization.json.Json.encodeToString(
                            kotlinx.serialization.json.JsonObject.serializer(),
                            kotlinx.serialization.json.JsonObject(it),
                        )
                    }.orEmpty(),
                    soundUri = a.soundUri,
                    volume = a.volume,
                    volumeRampSeconds = a.volumeRampSeconds,
                    vibrationEnabled = a.vibrationEnabled,
                    vibrationPattern = a.vibrationPattern.name,
                    snoozeEnabled = a.snoozeEnabled,
                    snoozeMinutes = a.snoozeMinutes,
                    snoozeRemaining = a.snoozeMaxCount,
                    triggeredAt = "",
                )
                scheduler.schedule(a, payload)
            }
            scheduledStore.save(activeIds)
            Result.success()
        }.getOrElse { e ->
            // 401은 재시도해도 같으니 그냥 종료. 다음 로그인 시 다시 enqueue됨.
            if (e is HttpException && e.code() == 401) {
                Timber.w("AlarmRescheduleWorker: 401, skipping")
                Result.success()
            } else {
                Timber.e(e, "Reschedule failed")
                Result.retry()
            }
        }
    }

    companion object {
        private const val UNIQUE_NAME = "alarm-reschedule"

        fun enqueue(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<AlarmRescheduleWorker>().build()
            WorkManager.getInstance(ctx)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, req)
        }
    }
}
