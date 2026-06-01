import { Injectable } from '@nestjs/common';
import { TeamRole } from '@prisma/client';
import { customAlphabet } from 'nanoid';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';
import { MembershipHelper } from '../teams/membership.helper';

// 헷갈리기 쉬운 글자 제외 (O, 0, I, 1, l 등)
const CODE_ALPHABET = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
const CODE_LENGTH = 8;
const generateCode = customAlphabet(CODE_ALPHABET, CODE_LENGTH);

@Injectable()
export class InvitesService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly membership: MembershipHelper,
  ) {}

  private inviteUrl(code: string) {
    return `${process.env.INVITE_LINK_BASE ?? 'https://finalalarm.app/i/'}${code}`;
  }

  async list(userId: string, teamId: string) {
    await this.membership.requireMember(userId, teamId);
    const rows = await this.prisma.teamInvite.findMany({
      where: {
        teamId,
        revokedAt: null,
        expiresAt: { gt: new Date() },
      },
      orderBy: { createdAt: 'desc' },
    });
    return rows.map((r) => ({ ...r, url: this.inviteUrl(r.code) }));
  }

  async create(userId: string, teamId: string, expiresInDays = 7, maxUses?: number) {
    await this.membership.requireRole(userId, teamId, [TeamRole.OWNER, TeamRole.ADMIN]);
    const code = generateCode();
    const expiresAt = new Date(Date.now() + expiresInDays * 24 * 60 * 60 * 1000);
    const invite = await this.prisma.teamInvite.create({
      data: {
        teamId,
        code,
        createdBy: userId,
        expiresAt,
        maxUses: maxUses ?? null,
      },
    });
    return { ...invite, url: this.inviteUrl(code) };
  }

  async revoke(userId: string, teamId: string, inviteId: string) {
    await this.membership.requireRole(userId, teamId, [TeamRole.OWNER, TeamRole.ADMIN]);
    const invite = await this.prisma.teamInvite.findUnique({ where: { id: inviteId } });
    if (!invite || invite.teamId !== teamId) {
      throw new AppError('NOT_FOUND', 'Invite not found');
    }
    if (invite.revokedAt) return;
    await this.prisma.teamInvite.update({
      where: { id: inviteId },
      data: { revokedAt: new Date() },
    });
  }

  async preview(code: string) {
    const invite = await this.prisma.teamInvite.findUnique({
      where: { code },
      include: {
        team: {
          select: {
            id: true,
            name: true,
            _count: { select: { members: true } },
          },
        },
      },
    });
    if (!invite) throw new AppError('INVITE_INVALID', 'Invite not found');
    this.assertUsable(invite);
    return {
      teamId: invite.team.id,
      teamName: invite.team.name,
      memberCount: invite.team._count.members,
      expiresAt: invite.expiresAt,
    };
  }

  async redeem(userId: string, code: string) {
    const invite = await this.prisma.teamInvite.findUnique({ where: { code } });
    if (!invite) throw new AppError('INVITE_INVALID', 'Invite not found');
    this.assertUsable(invite);

    return this.prisma.$transaction(async (tx) => {
      // 재확인 + use_count 원자적 증가
      const fresh = await tx.teamInvite.findUnique({ where: { id: invite.id } });
      if (!fresh) throw new AppError('INVITE_INVALID', 'Invite gone');
      this.assertUsable(fresh);

      const existing = await tx.teamMember.findUnique({
        where: { teamId_userId: { teamId: fresh.teamId, userId } },
      });
      if (existing) {
        // 이미 가입한 경우는 멱등하게 처리
        return { teamId: fresh.teamId, already: true };
      }

      await tx.teamMember.create({
        data: { teamId: fresh.teamId, userId, role: TeamRole.MEMBER },
      });
      await tx.teamInvite.update({
        where: { id: fresh.id },
        data: { useCount: { increment: 1 } },
      });
      return { teamId: fresh.teamId, already: false };
    });
  }

  private assertUsable(invite: {
    expiresAt: Date;
    revokedAt: Date | null;
    maxUses: number | null;
    useCount: number;
  }) {
    if (invite.revokedAt) throw new AppError('INVITE_EXPIRED', 'Invite revoked');
    if (invite.expiresAt < new Date()) throw new AppError('INVITE_EXPIRED', 'Invite expired');
    if (invite.maxUses != null && invite.useCount >= invite.maxUses) {
      throw new AppError('INVITE_EXPIRED', 'Invite usage exhausted');
    }
  }
}
