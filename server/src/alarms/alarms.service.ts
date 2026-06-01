import { Injectable } from '@nestjs/common';
import { AlarmKind, Prisma, ScheduleType } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';
import { MembershipHelper } from '../teams/membership.helper';
import { CreateAlarmDto, UpdateAlarmDto } from './dto';

@Injectable()
export class AlarmsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly membership: MembershipHelper,
  ) {}

  private validateSchedule(
    scheduleType: ScheduleType,
    oneShotAt?: string | null,
    timeOfDay?: string | null,
    daysOfWeek?: number | null,
  ) {
    if (scheduleType === ScheduleType.ONE_SHOT) {
      if (!oneShotAt) throw new AppError('VALIDATION_ERROR', 'oneShotAt required for ONE_SHOT');
      if (timeOfDay || daysOfWeek != null) {
        throw new AppError('VALIDATION_ERROR', 'timeOfDay/daysOfWeek must be null for ONE_SHOT');
      }
    } else {
      if (!timeOfDay || !daysOfWeek) {
        throw new AppError('VALIDATION_ERROR', 'timeOfDay and daysOfWeek required for RECURRING');
      }
      if (oneShotAt) {
        throw new AppError('VALIDATION_ERROR', 'oneShotAt must be null for RECURRING');
      }
    }
  }

  async list(
    userId: string,
    filters: { teamId?: string; kind?: AlarmKind; active?: boolean },
  ) {
    const memberships = await this.prisma.teamMember.findMany({
      where: { userId },
      select: { teamId: true },
    });
    const myTeamIds = memberships.map((m) => m.teamId);

    const where: Prisma.AlarmDefinitionWhereInput = {
      OR: [
        { ownerId: userId },
        ...(myTeamIds.length
          ? [
              {
                kind: AlarmKind.TEAM_APPROVAL,
                teamId: { in: myTeamIds },
              } as Prisma.AlarmDefinitionWhereInput,
            ]
          : []),
      ],
    };
    if (filters.teamId) (where as any).teamId = filters.teamId;
    if (filters.kind) (where as any).kind = filters.kind;
    if (filters.active != null) (where as any).active = filters.active;

    return this.prisma.alarmDefinition.findMany({
      where,
      orderBy: [{ active: 'desc' }, { createdAt: 'desc' }],
      include: {
        owner: { select: { id: true, displayName: true, avatarUrl: true } },
        team: { select: { id: true, name: true } },
      },
    });
  }

  async get(userId: string, id: string) {
    const a = await this.prisma.alarmDefinition.findUnique({
      where: { id },
      include: {
        owner: { select: { id: true, displayName: true, avatarUrl: true } },
        team: { select: { id: true, name: true } },
      },
    });
    if (!a) throw new AppError('NOT_FOUND', 'Alarm not found');

    if (a.ownerId !== userId) {
      if (a.kind !== AlarmKind.TEAM_APPROVAL || !a.teamId) {
        throw new AppError('FORBIDDEN', 'No access');
      }
      await this.membership.requireMember(userId, a.teamId);
    }
    return a;
  }

  async create(userId: string, dto: CreateAlarmDto) {
    if (dto.kind === AlarmKind.TEAM_APPROVAL) {
      if (!dto.teamId) throw new AppError('VALIDATION_ERROR', 'teamId required for TEAM_APPROVAL');
      await this.membership.requireMember(userId, dto.teamId);
    } else if (dto.teamId) {
      throw new AppError('VALIDATION_ERROR', 'teamId must be null for PERSONAL');
    }

    this.validateSchedule(dto.scheduleType, dto.oneShotAt, dto.timeOfDay, dto.daysOfWeek);

    const mission = await this.prisma.userMission.findUnique({ where: { id: dto.missionId } });
    if (!mission || mission.userId !== userId) {
      throw new AppError('VALIDATION_ERROR', 'Mission not owned by user');
    }

    return this.prisma.alarmDefinition.create({
      data: {
        ownerId: userId,
        teamId: dto.teamId ?? null,
        kind: dto.kind,
        label: dto.label,
        timezone: dto.timezone,
        scheduleType: dto.scheduleType,
        oneShotAt: dto.oneShotAt ? new Date(dto.oneShotAt) : null,
        timeOfDay: dto.timeOfDay ?? null,
        daysOfWeek: dto.daysOfWeek ?? null,
        soundUri: dto.soundUri,
        volume: dto.volume,
        volumeRampSeconds: dto.volumeRampSeconds,
        vibrationEnabled: dto.vibrationEnabled,
        vibrationPattern: dto.vibrationPattern,
        snoozeEnabled: dto.snoozeEnabled,
        snoozeMinutes: dto.snoozeMinutes,
        snoozeMaxCount: dto.snoozeMaxCount,
        missionId: dto.missionId,
      },
    });
  }

  async update(userId: string, id: string, dto: UpdateAlarmDto) {
    const a = await this.prisma.alarmDefinition.findUnique({ where: { id } });
    if (!a) throw new AppError('NOT_FOUND', 'Alarm not found');
    if (a.ownerId !== userId) throw new AppError('FORBIDDEN', 'Only owner can edit');

    // 스케줄 정합성 — 변경되는 경우만 재검증
    const scheduleType = dto.scheduleType ?? a.scheduleType;
    const oneShotAt = dto.oneShotAt !== undefined ? dto.oneShotAt : a.oneShotAt?.toISOString();
    const timeOfDay = dto.timeOfDay !== undefined ? dto.timeOfDay : a.timeOfDay;
    const daysOfWeek = dto.daysOfWeek !== undefined ? dto.daysOfWeek : a.daysOfWeek;
    this.validateSchedule(scheduleType, oneShotAt ?? null, timeOfDay, daysOfWeek);

    if (dto.missionId) {
      const m = await this.prisma.userMission.findUnique({ where: { id: dto.missionId } });
      if (!m || m.userId !== userId) {
        throw new AppError('VALIDATION_ERROR', 'Mission not owned by user');
      }
    }

    return this.prisma.alarmDefinition.update({
      where: { id },
      data: {
        label: dto.label,
        timezone: dto.timezone,
        scheduleType: dto.scheduleType,
        oneShotAt:
          dto.oneShotAt === null
            ? null
            : dto.oneShotAt !== undefined
              ? new Date(dto.oneShotAt)
              : undefined,
        timeOfDay: dto.timeOfDay === null ? null : dto.timeOfDay,
        daysOfWeek: dto.daysOfWeek === null ? null : dto.daysOfWeek,
        soundUri: dto.soundUri,
        volume: dto.volume,
        volumeRampSeconds: dto.volumeRampSeconds,
        vibrationEnabled: dto.vibrationEnabled,
        vibrationPattern: dto.vibrationPattern,
        snoozeEnabled: dto.snoozeEnabled,
        snoozeMinutes: dto.snoozeMinutes,
        snoozeMaxCount: dto.snoozeMaxCount,
        missionId: dto.missionId,
        active: dto.active,
      },
    });
  }

  async remove(userId: string, id: string) {
    const a = await this.prisma.alarmDefinition.findUnique({ where: { id } });
    if (!a) throw new AppError('NOT_FOUND', 'Alarm not found');
    if (a.ownerId !== userId) throw new AppError('FORBIDDEN', 'Only owner can delete');
    await this.prisma.alarmDefinition.delete({ where: { id } });
  }
}
