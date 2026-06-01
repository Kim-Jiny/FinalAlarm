package com.jiny.finalalarm.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jiny.finalalarm.data.api.AlarmDto
import com.jiny.finalalarm.data.api.ScheduleType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: AlarmDto, defaultPayload: AlarmRingPayload) {
        if (!alarm.active) return
        val triggerAt = nextTriggerEpochMillis(alarm) ?: return
        val launchIntent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            ?: Intent(Intent.ACTION_MAIN).apply { setPackage(ctx.packageName) }
        val showIntent = PendingIntent.getActivity(
            ctx,
            alarm.id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val fireIntent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtras(defaultPayload.toBundle())
        }
        val firePi = PendingIntent.getBroadcast(
            ctx,
            alarm.id.hashCode(),
            fireIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), firePi)
    }

    fun cancel(alarmId: String) {
        val fireIntent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            alarmId.hashCode(),
            fireIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
        if (pi != null) am.cancel(pi)
    }

    fun nextTriggerEpochMillis(alarm: AlarmDto): Long? {
        val zone = runCatching { ZoneId.of(alarm.timezone) }.getOrDefault(ZoneId.systemDefault())
        return when (alarm.scheduleType) {
            ScheduleType.ONE_SHOT -> {
                val ts = alarm.oneShotAt ?: return null
                runCatching { java.time.OffsetDateTime.parse(ts).toInstant().toEpochMilli() }.getOrNull()
            }
            ScheduleType.RECURRING -> {
                val tod = alarm.timeOfDay ?: return null
                val dow = alarm.daysOfWeek ?: return null
                val (h, m) = tod.split(":").map { it.toInt() }
                val now = ZonedDateTime.now(zone)
                var candidate = now.with(LocalTime.of(h, m, 0))
                if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
                repeat(7) {
                    val isoDow = candidate.dayOfWeek.value // 1..7 Mon..Sun
                    if ((dow shr (isoDow - 1)) and 1 == 1) {
                        return candidate.toInstant().toEpochMilli()
                    }
                    candidate = candidate.plusDays(1)
                }
                null
            }
        }
    }
}
