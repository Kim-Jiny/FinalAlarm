/**
 * OpenAPI 응답 스키마 — Prisma 모델의 거울. 클라이언트 코드 생성 입력.
 * Service들이 Prisma 객체 그대로 반환하지만, 응답 schema는 여기 정의된 타입과 일치해야 함.
 */
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  AlarmEventState,
  AlarmKind,
  DevicePlatform,
  MissionType,
  ScheduleType,
  TeamRole,
  UnlockRequestStatus,
  VibrationPattern,
} from '@prisma/client';

// ---------- Users ----------

export class UserDto {
  @ApiProperty() id!: string;
  @ApiProperty() email!: string;
  @ApiProperty() displayName!: string;
  @ApiPropertyOptional({ nullable: true }) avatarUrl!: string | null;
  @ApiPropertyOptional({ nullable: true }) timezone!: string | null;
  @ApiPropertyOptional() createdAt?: Date;
}

export class AuthResponseDto {
  @ApiProperty({ type: UserDto }) user!: UserDto;
  @ApiProperty() accessToken!: string;
  @ApiProperty() refreshToken!: string;
}

// ---------- Teams ----------

export class TeamSummaryDto {
  @ApiProperty() id!: string;
  @ApiProperty() name!: string;
  @ApiProperty({ enum: TeamRole }) role!: TeamRole;
  @ApiProperty() joinedAt!: Date;
}

export class TeamMemberUserDto {
  @ApiProperty() id!: string;
  @ApiProperty() displayName!: string;
  @ApiPropertyOptional({ nullable: true }) avatarUrl!: string | null;
  @ApiProperty() email!: string;
  @ApiPropertyOptional({ nullable: true }) timezone!: string | null;
}

export class LastAlarmSnapshotDto {
  @ApiProperty() id!: string;
  @ApiProperty() targetUserId!: string;
  @ApiProperty({ enum: AlarmEventState }) state!: AlarmEventState;
  @ApiProperty() triggeredAt!: Date;
  @ApiPropertyOptional({ nullable: true }) dismissedAt!: Date | null;
  @ApiPropertyOptional({ nullable: true }) volumePctAtTrigger!: number | null;
  @ApiPropertyOptional({ nullable: true }) dndAtTrigger!: boolean | null;
  @ApiPropertyOptional({ nullable: true }) volumePctAtDismiss!: number | null;
  @ApiPropertyOptional({ nullable: true }) dndAtDismiss!: boolean | null;
  @ApiPropertyOptional({ nullable: true }) lastSeenAt!: Date | null;
  @ApiPropertyOptional({ nullable: true }) liveVolumePct!: number | null;
  @ApiPropertyOptional({ nullable: true }) liveDnd!: boolean | null;
}

export class TeamMemberDto {
  @ApiProperty() teamId!: string;
  @ApiProperty() userId!: string;
  @ApiProperty({ enum: TeamRole }) role!: TeamRole;
  @ApiProperty() joinedAt!: Date;
  @ApiProperty({ type: TeamMemberUserDto }) user!: TeamMemberUserDto;
  @ApiPropertyOptional({ type: LastAlarmSnapshotDto, nullable: true })
  lastAlarmSnapshot!: LastAlarmSnapshotDto | null;
}

export class TeamDto {
  @ApiProperty() id!: string;
  @ApiProperty() name!: string;
  @ApiPropertyOptional({ nullable: true }) createdBy!: string | null;
  @ApiProperty() createdAt!: Date;
  @ApiProperty() updatedAt!: Date;
  @ApiProperty({ type: [TeamMemberDto] }) members!: TeamMemberDto[];
}

// ---------- Invites ----------

export class InviteDto {
  @ApiProperty() id!: string;
  @ApiProperty() teamId!: string;
  @ApiProperty() code!: string;
  @ApiProperty() url!: string;
  @ApiProperty() expiresAt!: Date;
  @ApiPropertyOptional({ nullable: true }) createdBy!: string | null;
  @ApiProperty() createdAt!: Date;
}

export class InvitePreviewDto {
  @ApiProperty() teamName!: string;
  @ApiProperty() expiresAt!: Date;
}

export class RedeemInviteDto {
  @ApiProperty() teamId!: string;
}

// ---------- Missions ----------

export class MissionDto {
  @ApiProperty() id!: string;
  @ApiProperty() userId!: string;
  @ApiProperty({ enum: MissionType }) type!: MissionType;
  @ApiProperty() name!: string;
  @ApiProperty({ type: 'object', additionalProperties: true })
  config!: Record<string, unknown>;
  @ApiProperty() isDefault!: boolean;
  @ApiProperty() createdAt!: Date;
  @ApiProperty() updatedAt!: Date;
}

// ---------- Alarms ----------

/** Alarm 응답에 함께 포함되는 owner 요약 (Prisma include) */
export class AlarmOwnerDto {
  @ApiProperty() id!: string;
  @ApiProperty() displayName!: string;
  @ApiPropertyOptional({ nullable: true }) avatarUrl!: string | null;
}

/** Alarm 응답에 함께 포함되는 team 요약 */
export class AlarmTeamDto {
  @ApiProperty() id!: string;
  @ApiProperty() name!: string;
}

export class AlarmDto {
  @ApiProperty() id!: string;
  @ApiProperty() ownerId!: string;
  @ApiProperty({ enum: AlarmKind }) kind!: AlarmKind;
  @ApiPropertyOptional({ nullable: true }) teamId!: string | null;
  @ApiProperty() label!: string;
  @ApiProperty() timezone!: string;
  @ApiProperty({ enum: ScheduleType }) scheduleType!: ScheduleType;
  @ApiPropertyOptional({ nullable: true }) timeOfDay!: string | null;
  @ApiPropertyOptional({ nullable: true }) daysOfWeek!: number | null;
  @ApiPropertyOptional({ nullable: true }) oneShotAt!: Date | null;
  @ApiProperty() soundUri!: string;
  @ApiProperty() volume!: number;
  @ApiProperty() volumeRampSeconds!: number;
  @ApiProperty() vibrationEnabled!: boolean;
  @ApiProperty({ enum: VibrationPattern }) vibrationPattern!: VibrationPattern;
  @ApiProperty() snoozeEnabled!: boolean;
  @ApiProperty() snoozeMinutes!: number;
  @ApiProperty() snoozeMaxCount!: number;
  @ApiProperty() missionId!: string;
  @ApiProperty() active!: boolean;
  @ApiProperty() createdAt!: Date;
  @ApiProperty() updatedAt!: Date;
  @ApiPropertyOptional({ type: AlarmOwnerDto, nullable: true })
  owner?: AlarmOwnerDto | null;
  @ApiPropertyOptional({ type: AlarmTeamDto, nullable: true })
  team?: AlarmTeamDto | null;
}

// ---------- Alarm Windows ----------

export class AlarmWindowDto {
  @ApiProperty() id!: string;
  @ApiProperty() teamId!: string;
  @ApiProperty() startTime!: string;
  @ApiProperty() endTime!: string;
  @ApiProperty() daysOfWeek!: number;
  @ApiProperty() timezone!: string;
  @ApiProperty() createdAt!: Date;
}

// ---------- Alarm Events ----------

export class AlarmEventDto {
  @ApiProperty() id!: string;
  @ApiPropertyOptional({ nullable: true }) definitionId!: string | null;
  @ApiPropertyOptional({ nullable: true }) windowId!: string | null;
  @ApiProperty() targetUserId!: string;
  @ApiPropertyOptional({ nullable: true }) senderUserId!: string | null;
  @ApiPropertyOptional({ nullable: true }) teamId!: string | null;
  @ApiProperty() missionId!: string;
  @ApiProperty({ enum: AlarmEventState }) state!: AlarmEventState;
  @ApiProperty() snoozeCount!: number;
  @ApiPropertyOptional({ nullable: true }) lastSnoozedAt!: Date | null;
  @ApiPropertyOptional({ nullable: true }) nextRingAt!: Date | null;
  @ApiProperty() triggeredAt!: Date;
  @ApiPropertyOptional({ nullable: true }) dismissedAt!: Date | null;
  @ApiPropertyOptional({ nullable: true }) volumePctAtTrigger!: number | null;
  @ApiPropertyOptional({ nullable: true }) dndAtTrigger!: boolean | null;
  @ApiPropertyOptional({ nullable: true }) volumePctAtDismiss!: number | null;
  @ApiPropertyOptional({ nullable: true }) dndAtDismiss!: boolean | null;
  @ApiPropertyOptional({ nullable: true }) lastSeenAt!: Date | null;
  @ApiPropertyOptional({ nullable: true }) liveVolumePct!: number | null;
  @ApiPropertyOptional({ nullable: true }) liveDnd!: boolean | null;
  @ApiProperty() createdAt!: Date;
  // Prisma include로 합쳐 보낼 때만 채워짐 (단건 조회 + active 리스트). history는 nested 없음.
  @ApiPropertyOptional({ type: AlarmDto, nullable: true })
  definition?: AlarmDto | null;
  @ApiPropertyOptional({ type: MissionDto, nullable: true })
  mission?: MissionDto | null;
  @ApiPropertyOptional({ type: AlarmOwnerDto, nullable: true })
  sender?: AlarmOwnerDto | null;
}

export class HeartbeatAckDto {
  @ApiProperty() ok!: boolean;
  @ApiPropertyOptional() ignored?: boolean;
}

// ---------- Unlock Requests ----------

export class UnlockRequestDto {
  @ApiProperty() id!: string;
  @ApiProperty() eventId!: string;
  @ApiProperty() requesterId!: string;
  @ApiProperty() teamId!: string;
  @ApiProperty({ enum: UnlockRequestStatus }) status!: UnlockRequestStatus;
  @ApiPropertyOptional({ nullable: true }) approvedBy!: string | null;
  @ApiPropertyOptional({ nullable: true }) approvedAt!: Date | null;
  @ApiProperty() expiresAt!: Date;
  @ApiProperty() createdAt!: Date;
}

// ---------- Push Tokens ----------

export class PushTokenDto {
  @ApiProperty() id!: string;
  @ApiProperty() userId!: string;
  @ApiProperty({ enum: DevicePlatform }) platform!: DevicePlatform;
  @ApiProperty() token!: string;
  @ApiProperty() deviceId!: string;
  @ApiProperty() createdAt!: Date;
  @ApiProperty() lastSeenAt!: Date;
}

// ---------- 빈 응답 (204) ----------

export class EmptyResponseDto {}
