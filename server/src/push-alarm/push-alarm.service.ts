import { Injectable } from '@nestjs/common';
import { AlarmEventState, AlarmKind } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';
import { MembershipHelper } from '../teams/membership.helper';
import { WindowsService } from '../windows/windows.service';
import { FcmService } from '../fcm/fcm.service';
import { alarmRingPayload } from '../fcm/payloads';
import { PushAlarmDto } from './dto';

@Injectable()
export class PushAlarmService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly membership: MembershipHelper,
    private readonly windows: WindowsService,
    private readonly fcm: FcmService,
  ) {}

  async push(senderId: string, dto: PushAlarmDto) {
    if (senderId === dto.targetUserId) {
      throw new AppError('VALIDATION_ERROR', 'Cannot push to yourself');
    }
    await this.membership.requireMember(senderId, dto.teamId);
    await this.membership.requireMember(dto.targetUserId, dto.teamId);

    const now = new Date();
    const window = await this.windows.findActiveAt(dto.targetUserId, dto.teamId, now);
    if (!window) throw new AppError('WINDOW_NOT_ACTIVE', 'Target has no active window now');

    // target의 기본 미션 (없으면 가장 최근 미션)
    const mission =
      (await this.prisma.userMission.findFirst({
        where: { userId: dto.targetUserId, isDefault: true },
      })) ??
      (await this.prisma.userMission.findFirst({
        where: { userId: dto.targetUserId },
        orderBy: { createdAt: 'desc' },
      }));
    if (!mission) throw new AppError('CONFLICT', 'Target has no mission profile');

    const event = await this.prisma.alarmEvent.create({
      data: {
        targetUserId: dto.targetUserId,
        senderUserId: senderId,
        teamId: dto.teamId,
        windowId: window.id,
        missionId: mission.id,
        state: AlarmEventState.RINGING,
        triggeredAt: now,
      },
    });

    const sender = await this.prisma.user.findUnique({
      where: { id: senderId },
      select: { id: true, displayName: true },
    });

    // 기본 사운드/진동은 target의 환경. push-alarm은 알람 정의가 없으므로 시스템 기본값으로.
    await this.fcm.sendToUser(
      dto.targetUserId,
      alarmRingPayload({
        event,
        alarmKind: AlarmKind.TEAM_APPROVAL,
        sender,
        label: dto.label ?? '팀원이 깨우는 중',
        mission,
        soundUri: 'system:default',
        volume: 80,
        volumeRampSeconds: 30,
        vibrationEnabled: true,
        vibrationPattern: 'PULSE',
        snoozeEnabled: false, // push 알람은 스누즈 비활성 (정책 선택지)
        snoozeMinutes: 5,
        snoozeRemaining: 0,
      }),
    );

    return { eventId: event.id };
  }
}
