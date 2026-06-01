// FCM data 메시지는 string 값만 허용. 객체는 직렬화해서 보냄.

import { AlarmEvent, AlarmKind, UnlockRequest, User, UserMission } from '@prisma/client';

export type FcmPayload = Record<string, string>;

export function alarmRingPayload(args: {
  event: AlarmEvent;
  alarmKind: AlarmKind;
  sender?: Pick<User, 'id' | 'displayName'> | null;
  label: string;
  mission: UserMission;
  soundUri: string;
  volume: number;
  volumeRampSeconds: number;
  vibrationEnabled: boolean;
  vibrationPattern: string;
  snoozeEnabled: boolean;
  snoozeMinutes: number;
  snoozeRemaining: number;
}): FcmPayload {
  return {
    type: 'ALARM_RING',
    eventId: args.event.id,
    alarmKind: args.alarmKind,
    senderUserId: args.sender?.id ?? '',
    senderDisplayName: args.sender?.displayName ?? '',
    label: args.label,
    missionId: args.mission.id,
    missionType: args.mission.type,
    missionConfig: JSON.stringify(args.mission.config),
    soundUri: args.soundUri,
    volume: String(args.volume),
    volumeRampSeconds: String(args.volumeRampSeconds),
    vibrationEnabled: String(args.vibrationEnabled),
    vibrationPattern: args.vibrationPattern,
    snoozeEnabled: String(args.snoozeEnabled),
    snoozeMinutes: String(args.snoozeMinutes),
    snoozeRemaining: String(args.snoozeRemaining),
    triggeredAt: args.event.triggeredAt.toISOString(),
  };
}

export function unlockRequestPayload(args: {
  request: UnlockRequest;
  requester: Pick<User, 'id' | 'displayName'>;
}): FcmPayload {
  return {
    type: 'UNLOCK_REQUEST',
    requestId: args.request.id,
    eventId: args.request.eventId,
    requesterUserId: args.requester.id,
    requesterDisplayName: args.requester.displayName,
    teamId: args.request.teamId,
    expiresAt: args.request.expiresAt.toISOString(),
  };
}

export function unlockApprovedPayload(args: {
  request: UnlockRequest;
  approver: Pick<User, 'id' | 'displayName'>;
}): FcmPayload {
  return {
    type: 'UNLOCK_APPROVED',
    requestId: args.request.id,
    eventId: args.request.eventId,
    approvedByUserId: args.approver.id,
    approvedByDisplayName: args.approver.displayName,
  };
}
