import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';
import { MembershipHelper } from '../teams/membership.helper';
import { CreateWindowDto, UpdateWindowDto } from './dto';
import { isDayActive, validateDowMask } from '../common/util/days-of-week';
import { parseTimeOfDay } from '../common/util/time';

@Injectable()
export class WindowsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly membership: MembershipHelper,
  ) {}

  async listMine(userId: string) {
    return this.prisma.alarmWindow.findMany({
      where: { userId },
      orderBy: [{ active: 'desc' }, { startTime: 'asc' }],
      include: { team: { select: { id: true, name: true } } },
    });
  }

  async listByTeam(userId: string, teamId: string) {
    await this.membership.requireMember(userId, teamId);
    return this.prisma.alarmWindow.findMany({
      where: { teamId },
      orderBy: [{ active: 'desc' }, { startTime: 'asc' }],
      include: {
        user: { select: { id: true, displayName: true, avatarUrl: true } },
      },
    });
  }

  async create(userId: string, dto: CreateWindowDto) {
    await this.membership.requireMember(userId, dto.teamId);
    if (!validateDowMask(dto.daysOfWeek)) {
      throw new AppError('VALIDATION_ERROR', 'invalid daysOfWeek');
    }
    return this.prisma.alarmWindow.create({
      data: {
        userId,
        teamId: dto.teamId,
        startTime: dto.startTime,
        endTime: dto.endTime,
        daysOfWeek: dto.daysOfWeek,
        timezone: dto.timezone,
      },
    });
  }

  async update(userId: string, id: string, dto: UpdateWindowDto) {
    const w = await this.prisma.alarmWindow.findUnique({ where: { id } });
    if (!w) throw new AppError('NOT_FOUND', 'Window not found');
    if (w.userId !== userId) throw new AppError('FORBIDDEN', 'Only owner can edit');
    if (dto.daysOfWeek != null && !validateDowMask(dto.daysOfWeek)) {
      throw new AppError('VALIDATION_ERROR', 'invalid daysOfWeek');
    }
    return this.prisma.alarmWindow.update({ where: { id }, data: dto });
  }

  async remove(userId: string, id: string) {
    const w = await this.prisma.alarmWindow.findUnique({ where: { id } });
    if (!w) throw new AppError('NOT_FOUND', 'Window not found');
    if (w.userId !== userId) throw new AppError('FORBIDDEN', 'Only owner can delete');
    await this.prisma.alarmWindow.delete({ where: { id } });
  }

  // Push alarm 모듈에서 사용: 현재 시각이 어떤 window 안에 있는지 검사.
  async findActiveAt(targetUserId: string, teamId: string, now: Date) {
    const windows = await this.prisma.alarmWindow.findMany({
      where: { userId: targetUserId, teamId, active: true },
    });
    return windows.find((w) => this.isCurrentlyActive(w, now));
  }

  private isCurrentlyActive(
    w: { startTime: string; endTime: string; daysOfWeek: number; timezone: string },
    now: Date,
  ): boolean {
    // 사용자 TZ 기준으로 요일·시각 계산. 단순화: server-side Intl.DateTimeFormat 사용.
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: w.timezone,
      weekday: 'short',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(now);
    const weekdayPart = parts.find((p) => p.type === 'weekday')?.value;
    const hour = Number(parts.find((p) => p.type === 'hour')?.value ?? '0');
    const minute = Number(parts.find((p) => p.type === 'minute')?.value ?? '0');
    const isoDowMap: Record<string, number> = {
      Mon: 1, Tue: 2, Wed: 3, Thu: 4, Fri: 5, Sat: 6, Sun: 7,
    };
    const isoDow = isoDowMap[weekdayPart ?? 'Mon'];
    if (!isDayActive(w.daysOfWeek, isoDow)) return false;

    const nowMin = hour * 60 + minute;
    const { h: sh, m: sm } = parseTimeOfDay(w.startTime);
    const { h: eh, m: em } = parseTimeOfDay(w.endTime);
    const startMin = sh * 60 + sm;
    const endMin = eh * 60 + em;

    if (startMin <= endMin) {
      return nowMin >= startMin && nowMin < endMin;
    }
    // 자정 넘김
    return nowMin >= startMin || nowMin < endMin;
  }
}
