import { Injectable } from '@nestjs/common';
import { AlarmEventState, UnlockRequestStatus } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';
import { MembershipHelper } from '../teams/membership.helper';
import { FcmService } from '../fcm/fcm.service';
import { unlockApprovedPayload } from '../fcm/payloads';

@Injectable()
export class UnlockRequestsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly membership: MembershipHelper,
    private readonly fcm: FcmService,
  ) {}

  async inbox(userId: string, teamId: string, status: UnlockRequestStatus = UnlockRequestStatus.PENDING) {
    await this.membership.requireMember(userId, teamId);
    return this.prisma.unlockRequest.findMany({
      where: {
        teamId,
        status,
        NOT: { requesterId: userId }, // 본인 요청 제외
        // lazy expiration: PENDING 조회 시 만료 안 된 것만
        ...(status === UnlockRequestStatus.PENDING
          ? { expiresAt: { gt: new Date() } }
          : {}),
      },
      orderBy: { createdAt: 'desc' },
      include: {
        requester: { select: { id: true, displayName: true, avatarUrl: true } },
        event: { select: { id: true, triggeredAt: true, state: true } },
      },
    });
  }

  async get(userId: string, id: string) {
    const r = await this.prisma.unlockRequest.findUnique({
      where: { id },
      include: {
        requester: { select: { id: true, displayName: true, avatarUrl: true } },
        event: true,
      },
    });
    if (!r) throw new AppError('NOT_FOUND', 'Request not found');
    // requester 본인이거나 같은 팀 멤버이면 조회 가능
    if (r.requesterId !== userId) {
      await this.membership.requireMember(userId, r.teamId);
    }
    return r;
  }

  async approve(userId: string, id: string) {
    const r = await this.prisma.unlockRequest.findUnique({ where: { id } });
    if (!r) throw new AppError('NOT_FOUND', 'Request not found');
    if (r.status !== UnlockRequestStatus.PENDING) {
      throw new AppError('REQUEST_INVALID_STATE', `Cannot approve in state ${r.status}`);
    }
    if (r.expiresAt < new Date()) {
      throw new AppError('REQUEST_EXPIRED', 'Request expired');
    }
    if (r.requesterId === userId) {
      throw new AppError('FORBIDDEN', 'Cannot approve own request');
    }
    await this.membership.requireMember(userId, r.teamId);

    const updated = await this.prisma.$transaction(async (tx) => {
      // 원자적: PENDING 인 상태만 업데이트
      const result = await tx.unlockRequest.updateMany({
        where: { id, status: UnlockRequestStatus.PENDING },
        data: {
          status: UnlockRequestStatus.APPROVED,
          approvedBy: userId,
          approvedAt: new Date(),
        },
      });
      if (result.count === 0) {
        throw new AppError('REQUEST_INVALID_STATE', 'Race lost; already updated');
      }
      await tx.alarmEvent.update({
        where: { id: r.eventId },
        data: { state: AlarmEventState.UNLOCK_APPROVED },
      });
      return tx.unlockRequest.findUniqueOrThrow({ where: { id } });
    });

    const approver = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { id: true, displayName: true },
    });
    if (approver) {
      await this.fcm.sendToUser(
        r.requesterId,
        unlockApprovedPayload({ request: updated, approver }),
      );
    }
    return updated;
  }

  async cancel(userId: string, id: string) {
    const r = await this.prisma.unlockRequest.findUnique({ where: { id } });
    if (!r) throw new AppError('NOT_FOUND', 'Request not found');
    if (r.requesterId !== userId) throw new AppError('FORBIDDEN', 'Only requester can cancel');
    if (r.status !== UnlockRequestStatus.PENDING) {
      throw new AppError('REQUEST_INVALID_STATE', `Cannot cancel in state ${r.status}`);
    }
    return this.prisma.$transaction(async (tx) => {
      await tx.unlockRequest.update({
        where: { id },
        data: { status: UnlockRequestStatus.CANCELED },
      });
      // 이벤트 state 되돌리기 (RINGING)
      await tx.alarmEvent.update({
        where: { id: r.eventId },
        data: { state: AlarmEventState.RINGING },
      });
    });
  }

}
