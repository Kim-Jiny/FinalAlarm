package com.jiny.finalalarm.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- Auth ----------
@Serializable
data class TokenPair(val accessToken: String, val refreshToken: String)

@Serializable
data class AuthResponse(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class SignupReq(
    val email: String,
    val password: String,
    val displayName: String,
    val timezone: String? = null,
)

@Serializable
data class LoginReq(val email: String, val password: String)

@Serializable
data class RefreshReq(val refreshToken: String)

@Serializable
data class LogoutReq(val refreshToken: String)

// ---------- User / Me ----------
@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val timezone: String,
)

@Serializable
data class UpdateMeReq(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val timezone: String? = null,
)

@Serializable
data class ChangePasswordReq(val currentPassword: String, val newPassword: String)

// ---------- Push tokens ----------
@Serializable
enum class DevicePlatform { ANDROID, IOS }

@Serializable
data class RegisterPushTokenReq(
    val platform: DevicePlatform,
    val token: String,
    val deviceId: String,
)

@Serializable
data class PushTokenDto(
    val id: String,
    val userId: String,
    val platform: DevicePlatform,
    val token: String,
    val deviceId: String,
)

// ---------- Teams ----------
@Serializable
enum class TeamRole { OWNER, ADMIN, MEMBER }

@Serializable
data class TeamSummary(
    val id: String,
    val name: String,
    val role: TeamRole,
    val joinedAt: String,
)

@Serializable
data class TeamDetail(
    val id: String,
    val name: String,
    val createdBy: String,
    val members: List<TeamMemberDto>,
)

@Serializable
data class TeamMemberDto(
    val teamId: String,
    val userId: String,
    val role: TeamRole,
    val joinedAt: String,
    val user: UserDto,
)

@Serializable
data class CreateTeamReq(val name: String)

@Serializable
data class UpdateTeamReq(val name: String? = null)

@Serializable
data class ChangeRoleReq(val role: TeamRole)

// ---------- Invites ----------
@Serializable
data class InviteDto(
    val id: String,
    val teamId: String,
    val code: String,
    val url: String,
    val expiresAt: String,
    val maxUses: Int? = null,
    val useCount: Int = 0,
)

@Serializable
data class CreateInviteReq(
    val expiresInDays: Int? = null,
    val maxUses: Int? = null,
)

@Serializable
data class InvitePreviewDto(
    val teamId: String,
    val teamName: String,
    val memberCount: Int,
    val expiresAt: String,
)

@Serializable
data class RedeemResult(val teamId: String, val already: Boolean)

// ---------- Missions ----------
@Serializable
enum class MissionType { MATH, PHOTO, SHAKE }

@Serializable
data class MissionDto(
    val id: String,
    val userId: String,
    val type: MissionType,
    val name: String,
    val config: Map<String, kotlinx.serialization.json.JsonElement>,
    val isDefault: Boolean,
)

@Serializable
data class CreateMissionReq(
    val type: MissionType,
    val name: String,
    val config: Map<String, kotlinx.serialization.json.JsonElement>,
    val isDefault: Boolean = false,
)

@Serializable
data class UpdateMissionReq(
    val name: String? = null,
    val config: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    val isDefault: Boolean? = null,
)

// ---------- Alarms ----------
@Serializable
enum class AlarmKind { TEAM_APPROVAL, PERSONAL }

@Serializable
enum class ScheduleType { ONE_SHOT, RECURRING }

@Serializable
enum class VibrationPattern { SHORT, MEDIUM, LONG, PULSE, HEARTBEAT }

@Serializable
data class AlarmDto(
    val id: String,
    val ownerId: String,
    val teamId: String? = null,
    val kind: AlarmKind,
    val label: String,
    val timezone: String,
    val scheduleType: ScheduleType,
    val oneShotAt: String? = null,
    val timeOfDay: String? = null,
    val daysOfWeek: Int? = null,
    val soundUri: String,
    val volume: Int,
    val volumeRampSeconds: Int,
    val vibrationEnabled: Boolean,
    val vibrationPattern: VibrationPattern,
    val snoozeEnabled: Boolean,
    val snoozeMinutes: Int,
    val snoozeMaxCount: Int,
    val missionId: String,
    val active: Boolean,
)

@Serializable
data class CreateAlarmReq(
    val kind: AlarmKind,
    val teamId: String? = null,
    val label: String,
    val timezone: String,
    val scheduleType: ScheduleType,
    val oneShotAt: String? = null,
    val timeOfDay: String? = null,
    val daysOfWeek: Int? = null,
    val soundUri: String,
    val volume: Int,
    val volumeRampSeconds: Int,
    val vibrationEnabled: Boolean,
    val vibrationPattern: VibrationPattern,
    val snoozeEnabled: Boolean,
    val snoozeMinutes: Int,
    val snoozeMaxCount: Int,
    val missionId: String,
)

// ---------- Windows ----------
@Serializable
data class WindowDto(
    val id: String,
    val userId: String,
    val teamId: String,
    val startTime: String,
    val endTime: String,
    val daysOfWeek: Int,
    val timezone: String,
    val active: Boolean,
)

@Serializable
data class CreateWindowReq(
    val teamId: String,
    val startTime: String,
    val endTime: String,
    val daysOfWeek: Int,
    val timezone: String,
)

// ---------- Push alarm ----------
@Serializable
data class PushAlarmReq(
    val targetUserId: String,
    val teamId: String,
    val label: String? = null,
)

@Serializable
data class PushAlarmResp(val eventId: String)

// ---------- Events ----------
@Serializable
enum class AlarmEventState {
    RINGING, SNOOZED, UNLOCK_REQUESTED, UNLOCK_APPROVED, DISMISSED, EXPIRED
}

@Serializable
data class AlarmEventDto(
    val id: String,
    val definitionId: String? = null,
    val windowId: String? = null,
    val targetUserId: String,
    val senderUserId: String? = null,
    val teamId: String? = null,
    val missionId: String,
    val state: AlarmEventState,
    val snoozeCount: Int,
    val triggeredAt: String,
    val nextRingAt: String? = null,
    val dismissedAt: String? = null,
    val volumePctAtTrigger: Int? = null,
    val dndAtTrigger: Boolean? = null,
    val volumePctAtDismiss: Int? = null,
    val dndAtDismiss: Boolean? = null,
)

@Serializable
data class DismissReq(
    val missionProof: Map<String, kotlinx.serialization.json.JsonElement>,
    val volumePct: Int? = null,
    val dnd: Boolean? = null,
)

@Serializable
data class CreateEventReq(
    val definitionId: String,
    val triggeredAt: String? = null,
    val initialState: String? = null,    // "RINGING" | "DISMISSED" (reconcile)
    val dismissedAt: String? = null,
    val volumePctAtTrigger: Int? = null,
    val dndAtTrigger: Boolean? = null,
    val volumePctAtDismiss: Int? = null,
    val dndAtDismiss: Boolean? = null,
)

// ---------- Unlock requests ----------
@Serializable
enum class UnlockRequestStatus { PENDING, APPROVED, EXPIRED, CANCELED }

@Serializable
data class UnlockRequestDto(
    val id: String,
    val eventId: String,
    val requesterId: String,
    val teamId: String,
    val status: UnlockRequestStatus,
    val approvedBy: String? = null,
    val approvedAt: String? = null,
    val expiresAt: String,
    val createdAt: String,
)

// ---------- Errors ----------
@Serializable
data class ApiError(val error: ErrorBody)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val details: kotlinx.serialization.json.JsonElement? = null,
)
