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
import com.jiny.finalalarm.data.api.FinalAlarmApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class AlarmRescheduleWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val api: FinalAlarmApi,
    private val scheduler: AlarmScheduler,
    private val scheduledStore: ScheduledAlarmStore,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = runCatching {
        val alarms = api.listAlarms(active = true)
        val activeIds = alarms.map { it.id }.toSet()

        // 더 이상 active 아닌 알람들 cancel
        val previouslyScheduled = scheduledStore.list()
        (previouslyScheduled - activeIds).forEach { scheduler.cancel(it) }

        // 미션 캐시 (알람별 fetch 중복 방지)
        val missionsById = runCatching { api.listMissions() }.getOrDefault(emptyList())
            .associateBy { it.id }

        alarms.forEach { a ->
            val mission = missionsById[a.missionId]
            val payload = AlarmRingPayload(
                eventId = "",                          // 빈 값 → ForegroundService가 발사 시 createEvent 호출
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
    }.getOrElse {
        Timber.e(it, "Reschedule failed")
        Result.retry()
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
