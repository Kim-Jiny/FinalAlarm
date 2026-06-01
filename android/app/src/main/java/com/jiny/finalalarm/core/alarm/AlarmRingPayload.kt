package com.jiny.finalalarm.core.alarm

import android.os.Bundle

// FCM ALARM_RING payload를 다루는 헬퍼.
// FCM data 메시지는 string-only이므로 그대로 보존하면서 사용 시 형 변환.

data class AlarmRingPayload(
    val eventId: String,             // 빈 문자열이면 로컬 발사 → ForegroundService가 서버에 createEvent 호출
    val definitionId: String?,       // 로컬 발사일 때만 set (서버에 보고용)
    val alarmKind: String,           // TEAM_APPROVAL | PERSONAL
    val senderUserId: String?,
    val senderDisplayName: String?,
    val label: String,
    val missionId: String,
    val missionType: String,         // MATH | PHOTO | SHAKE
    val missionConfigJson: String,
    val soundUri: String,
    val volume: Int,
    val volumeRampSeconds: Int,
    val vibrationEnabled: Boolean,
    val vibrationPattern: String,
    val snoozeEnabled: Boolean,
    val snoozeMinutes: Int,
    val snoozeRemaining: Int,
    val triggeredAt: String,
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString("eventId", eventId)
        putString("definitionId", definitionId)
        putString("alarmKind", alarmKind)
        putString("senderUserId", senderUserId)
        putString("senderDisplayName", senderDisplayName)
        putString("label", label)
        putString("missionId", missionId)
        putString("missionType", missionType)
        putString("missionConfigJson", missionConfigJson)
        putString("soundUri", soundUri)
        putInt("volume", volume)
        putInt("volumeRampSeconds", volumeRampSeconds)
        putBoolean("vibrationEnabled", vibrationEnabled)
        putString("vibrationPattern", vibrationPattern)
        putBoolean("snoozeEnabled", snoozeEnabled)
        putInt("snoozeMinutes", snoozeMinutes)
        putInt("snoozeRemaining", snoozeRemaining)
        putString("triggeredAt", triggeredAt)
    }

    companion object {
        fun fromDataMap(data: Map<String, String>): AlarmRingPayload = AlarmRingPayload(
            eventId = data["eventId"].orEmpty(),
            definitionId = data["definitionId"]?.takeIf { it.isNotEmpty() },
            alarmKind = data["alarmKind"].orEmpty(),
            senderUserId = data["senderUserId"]?.takeIf { it.isNotEmpty() },
            senderDisplayName = data["senderDisplayName"]?.takeIf { it.isNotEmpty() },
            label = data["label"].orEmpty(),
            missionId = data["missionId"].orEmpty(),
            missionType = data["missionType"].orEmpty(),
            missionConfigJson = data["missionConfig"].orEmpty(),
            soundUri = data["soundUri"].orEmpty(),
            volume = data["volume"]?.toIntOrNull() ?: 80,
            volumeRampSeconds = data["volumeRampSeconds"]?.toIntOrNull() ?: 0,
            vibrationEnabled = data["vibrationEnabled"]?.toBooleanStrictOrNull() ?: true,
            vibrationPattern = data["vibrationPattern"] ?: "PULSE",
            snoozeEnabled = data["snoozeEnabled"]?.toBooleanStrictOrNull() ?: false,
            snoozeMinutes = data["snoozeMinutes"]?.toIntOrNull() ?: 5,
            snoozeRemaining = data["snoozeRemaining"]?.toIntOrNull() ?: 0,
            triggeredAt = data["triggeredAt"].orEmpty(),
        )

        fun fromBundle(b: Bundle): AlarmRingPayload = AlarmRingPayload(
            eventId = b.getString("eventId").orEmpty(),
            definitionId = b.getString("definitionId"),
            alarmKind = b.getString("alarmKind").orEmpty(),
            senderUserId = b.getString("senderUserId"),
            senderDisplayName = b.getString("senderDisplayName"),
            label = b.getString("label").orEmpty(),
            missionId = b.getString("missionId").orEmpty(),
            missionType = b.getString("missionType").orEmpty(),
            missionConfigJson = b.getString("missionConfigJson").orEmpty(),
            soundUri = b.getString("soundUri").orEmpty(),
            volume = b.getInt("volume", 80),
            volumeRampSeconds = b.getInt("volumeRampSeconds", 0),
            vibrationEnabled = b.getBoolean("vibrationEnabled", true),
            vibrationPattern = b.getString("vibrationPattern", "PULSE"),
            snoozeEnabled = b.getBoolean("snoozeEnabled", false),
            snoozeMinutes = b.getInt("snoozeMinutes", 5),
            snoozeRemaining = b.getInt("snoozeRemaining", 0),
            triggeredAt = b.getString("triggeredAt").orEmpty(),
        )
    }
}
