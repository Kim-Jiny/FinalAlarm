import { Injectable } from '@nestjs/common';
import {
  AlarmEventState,
  AlarmKind,
  MissionType,
  UnlockRequestStatus,
} from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';
import { FcmService } from '../fcm/fcm.service';
import { unlockRequestPayload } from '../fcm/payloads';

const UNLOCK_REQUEST_TTL_MS = 5 * 60 * 1000;

@Injectable()
export class EventsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly fcm: FcmService,
  ) {}

  // 클라 로컬 알람 발사 보고 — definition 기준으로 event 생성
  // reconcile: initialState='DISMISSED'면 끈 후 도착한 이벤트로 처리
  async createFromDefinition(
    userId: string,
    definitionId: string,
    opts: { triggeredAt?: string; initialState?: 'RINGING' | 'DISMISSED'; dismissedAt?: string } = {},
  ) {
    const def = await this.prisma.alarmDefinition.findUnique({ where: { id: definitionId } });
    if (!def) throw new AppError('NOT_FOUND', 'Alarm not found');
    if (def.ownerId !== userId) throw new AppError('FORBIDDEN', 'Only owner can report');
    // Inactive 알람도 reconcile 케이스에서는 허용 (사용자가 알람 끈 후 정의를 비활성했을 수 있음)
    if (!def.active && opts.initialState !== 'DISMISSED') {
      throw new AppError('EVENT_INVALID_STATE', 'Alarm is inactive');
    }

    const dismissed = opts.initialState === 'DISMISSED';
    return this.prisma.alarmEvent.create({
      data: {
        definitionId: def.id,
        targetUserId: def.ownerId,
        teamId: def.teamId,
        missionId: def.missionId,
        state: dismissed ? AlarmEventState.DISMISSED : AlarmEventState.RINGING,
        triggeredAt: opts.triggeredAt ? new Date(opts.triggeredAt) : new Date(),
        dismissedAt: dismissed
          ? opts.dismissedAt
            ? new Date(opts.dismissedAt)
            : new Date()
          : null,
      },
    });
  }

  async listActive(userId: string) {
    return this.prisma.alarmEvent.findMany({
      where: {
        targetUserId: userId,
        state: { in: [AlarmEventState.RINGING, AlarmEventState.SNOOZED, AlarmEventState.UNLOCK_REQUESTED, AlarmEventState.UNLOCK_APPROVED] },
      },
      orderBy: { triggeredAt: 'desc' },
      include: {
        definition: true,
        sender: { select: { id: true, displayName: true, avatarUrl: true } },
        mission: true,
      },
    });
  }

  async get(userId: string, id: string) {
    const e = await this.prisma.alarmEvent.findUnique({
      where: { id },
      include: {
        definition: true,
        sender: { select: { id: true, displayName: true, avatarUrl: true } },
        mission: true,
      },
    });
    if (!e) throw new AppError('NOT_FOUND', 'Event not found');
    if (e.targetUserId !== userId) throw new AppError('FORBIDDEN', 'No access');
    return e;
  }

  async history(userId: string, from?: string, to?: string, limit = 50) {
    const where: any = { targetUserId: userId };
    if (from) where.triggeredAt = { ...(where.triggeredAt ?? {}), gte: new Date(from) };
    if (to) where.triggeredAt = { ...(where.triggeredAt ?? {}), lte: new Date(to) };
    return this.prisma.alarmEvent.findMany({
      where,
      orderBy: { triggeredAt: 'desc' },
      take: Math.min(limit, 200),
    });
  }

  async snooze(userId: string, id: string) {
    const event = await this.prisma.alarmEvent.findUnique({
      where: { id },
      include: { definition: true },
    });
    if (!event) throw new AppError('NOT_FOUND', 'Event not found');
    if (event.targetUserId !== userId) throw new AppError('FORBIDDEN', 'No access');
    if (event.state !== AlarmEventState.RINGING) {
      throw new AppError('EVENT_INVALID_STATE', `Cannot snooze in state ${event.state}`);
    }

    const def = event.definition;
    if (!def?.snoozeEnabled) {
      throw new AppError('EVENT_INVALID_STATE', 'Snooze disabled for this alarm');
    }
    if (def.snoozeMaxCount !== -1 && event.snoozeCount >= def.snoozeMaxCount) {
      throw new AppError('EVENT_INVALID_STATE', 'Snooze limit reached');
    }

    const now = new Date();
    const nextRingAt = new Date(now.getTime() + def.snoozeMinutes * 60_000);
    return this.prisma.alarmEvent.update({
      where: { id },
      data: {
        state: AlarmEventState.SNOOZED,
        snoozeCount: { increment: 1 },
        lastSnoozedAt: now,
        nextRingAt,
      },
    });
  }

  async dismiss(userId: string, id: string, missionProof: { type: MissionType; [k: string]: unknown }) {
    const event = await this.prisma.alarmEvent.findUnique({
      where: { id },
      include: { definition: true, mission: true },
    });
    if (!event) throw new AppError('NOT_FOUND', 'Event not found');
    if (event.targetUserId !== userId) throw new AppError('FORBIDDEN', 'No access');

    const isTeamApproval =
      event.definition?.kind === AlarmKind.TEAM_APPROVAL || event.senderUserId != null;
    if (isTeamApproval) {
      if (event.state !== AlarmEventState.UNLOCK_APPROVED) {
        throw new AppError('EVENT_INVALID_STATE', 'Team approval required first');
      }
    } else {
      if (event.state !== AlarmEventState.RINGING && event.state !== AlarmEventState.SNOOZED) {
        throw new AppError('EVENT_INVALID_STATE', `Cannot dismiss in state ${event.state}`);
      }
    }

    if (missionProof.type !== event.mission.type) {
      throw new AppError('MISSION_FAILED', 'Mission type mismatch');
    }
    this.verifyMissionProof(event.mission.type, missionProof);

    return this.prisma.alarmEvent.update({
      where: { id },
      data: {
        state: AlarmEventState.DISMISSED,
        dismissedAt: new Date(),
        nextRingAt: null,
      },
    });
  }

  private verifyMissionProof(type: MissionType, proof: Record<string, unknown>) {
    // 가벼운 형식 검증. 강한 검증은 후속 단계에서.
    switch (type) {
      case MissionType.MATH:
        if (!Array.isArray((proof as any).answers)) {
          throw new AppError('MISSION_FAILED', 'answers[] required');
        }
        break;
      case MissionType.PHOTO:
        if (typeof (proof as any).imageUrl !== 'string') {
          throw new AppError('MISSION_FAILED', 'imageUrl required');
        }
        break;
      case MissionType.SHAKE:
        if (typeof (proof as any).shakeCount !== 'number') {
          throw new AppError('MISSION_FAILED', 'shakeCount required');
        }
        break;
    }
  }

  async requestUnlock(userId: string, id: string) {
    const event = await this.prisma.alarmEvent.findUnique({
      where: { id },
      include: { definition: true },
    });
    if (!event) throw new AppError('NOT_FOUND', 'Event not found');
    if (event.targetUserId !== userId) throw new AppError('FORBIDDEN', 'No access');

    const isTeamApproval =
      event.definition?.kind === AlarmKind.TEAM_APPROVAL || event.senderUserId != null;
    if (!isTeamApproval) {
      throw new AppError('EVENT_INVALID_STATE', 'Only team-approval alarms');
    }
    // RINGING / SNOOZED / 이전 요청 만료된 UNLOCK_REQUESTED에서 재요청 허용
    const allowedStates: AlarmEventState[] = [
      AlarmEventState.RINGING,
      AlarmEventState.SNOOZED,
      AlarmEventState.UNLOCK_REQUESTED,
    ];
    if (!allowedStates.includes(event.state)) {
      throw new AppError('EVENT_INVALID_STATE', `Cannot request unlock in state ${event.state}`);
    }
    if (!event.teamId) throw new AppError('CONFLICT', 'No team on event');

    // 1 PENDING per event — 만료된 것은 무시 (lazy expiration)
    const now = new Date();
    const existing = await this.prisma.unlockRequest.findFirst({
      where: {
        eventId: id,
        status: UnlockRequestStatus.PENDING,
        expiresAt: { gt: now },
      },
    });
    if (existing) {
      throw new AppError('CONFLICT', 'Pending request already exists');
    }

    const request = await this.prisma.$transaction(async (tx) => {
      const req = await tx.unlockRequest.create({
        data: {
          eventId: id,
          requesterId: userId,
          teamId: event.teamId!,
          status: UnlockRequestStatus.PENDING,
          expiresAt: new Date(now.getTime() + UNLOCK_REQUEST_TTL_MS),
        },
      });
      await tx.alarmEvent.update({
        where: { id },
        data: { state: AlarmEventState.UNLOCK_REQUESTED },
      });
      return req;
    });

    // 같은 팀의 다른 멤버에게 푸시 (요청자 본인 제외)
    const teammates = await this.prisma.teamMember.findMany({
      where: { teamId: event.teamId, NOT: { userId } },
      select: { userId: true },
    });
    const requester = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { id: true, displayName: true },
    });
    if (requester) {
      await Promise.all(
        teammates.map((m) =>
          this.fcm.sendToUser(m.userId, unlockRequestPayload({ request, requester })),
        ),
      );
    }
    return request;
  }
}
