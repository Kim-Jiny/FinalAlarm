package app.finalalarm.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.finalalarm.core.alarm.AlarmRingPayload
import app.finalalarm.core.alarm.AlarmScheduler
import app.finalalarm.data.api.FinalAlarmApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class AlarmRescheduleWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val api: FinalAlarmApi,
    private val scheduler: AlarmScheduler,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = runCatching {
        val alarms = api.listAlarms(active = true)
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
        Result.success()
    }.getOrElse {
        Timber.e(it, "Reschedule failed")
        Result.retry()
    }
}
