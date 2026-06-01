import { Injectable } from '@nestjs/common';
import { TeamRole } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';
import { MembershipHelper } from './membership.helper';

@Injectable()
export class TeamsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly membership: MembershipHelper,
  ) {}

  async listMyTeams(userId: string) {
    const rows = await this.prisma.teamMember.findMany({
      where: { userId },
      include: { team: true },
      orderBy: { joinedAt: 'desc' },
    });
    return rows.map((r) => ({
      id: r.team.id,
      name: r.team.name,
      role: r.role,
      joinedAt: r.joinedAt,
    }));
  }

  async createTeam(userId: string, name: string) {
    return this.prisma.$transaction(async (tx) => {
      const team = await tx.team.create({
        data: { name, createdBy: userId },
      });
      await tx.teamMember.create({
        data: { teamId: team.id, userId, role: TeamRole.OWNER },
      });
      return team;
    });
  }

  async getTeam(userId: string, teamId: string) {
    await this.membership.requireMember(userId, teamId);
    const team = await this.prisma.team.findUnique({
      where: { id: teamId },
      include: {
        members: {
          include: {
            user: { select: { id: true, displayName: true, avatarUrl: true, email: true } },
          },
          orderBy: { joinedAt: 'asc' },
        },
      },
    });
    if (!team) throw new AppError('NOT_FOUND', 'Team not found');
    return team;
  }

  async updateTeam(userId: string, teamId: string, name?: string) {
    await this.membership.requireRole(userId, teamId, [TeamRole.OWNER, TeamRole.ADMIN]);
    return this.prisma.team.update({
      where: { id: teamId },
      data: { name },
    });
  }

  async deleteTeam(userId: string, teamId: string) {
    await this.membership.requireRole(userId, teamId, [TeamRole.OWNER]);
    await this.prisma.team.delete({ where: { id: teamId } });
  }

  async leaveTeam(userId: string, teamId: string) {
    const role = await this.membership.requireMember(userId, teamId);
    if (role === TeamRole.OWNER) {
      const ownerCount = await this.prisma.teamMember.count({
        where: { teamId, role: TeamRole.OWNER },
      });
      if (ownerCount <= 1) {
        throw new AppError('CONFLICT', 'Owner cannot leave; transfer ownership or delete team');
      }
    }
    await this.prisma.teamMember.delete({
      where: { teamId_userId: { teamId, userId } },
    });
  }

  async kickMember(actorId: string, teamId: string, targetUserId: string) {
    if (actorId === targetUserId) {
      throw new AppError('VALIDATION_ERROR', 'Use leave endpoint for self');
    }
    const actorRole = await this.membership.requireRole(actorId, teamId, [
      TeamRole.OWNER,
      TeamRole.ADMIN,
    ]);
    const target = await this.prisma.teamMember.findUnique({
      where: { teamId_userId: { teamId, userId: targetUserId } },
    });
    if (!target) throw new AppError('NOT_FOUND', 'Target not in team');
    if (target.role === TeamRole.OWNER) {
      throw new AppError('FORBIDDEN', 'Cannot kick owner');
    }
    if (actorRole === TeamRole.ADMIN && target.role === TeamRole.ADMIN) {
      throw new AppError('FORBIDDEN', 'Admin cannot kick another admin');
    }
    await this.prisma.teamMember.delete({
      where: { teamId_userId: { teamId, userId: targetUserId } },
    });
  }

  async changeRole(actorId: string, teamId: string, targetUserId: string, role: TeamRole) {
    await this.membership.requireRole(actorId, teamId, [TeamRole.OWNER]);
    const target = await this.prisma.teamMember.findUnique({
      where: { teamId_userId: { teamId, userId: targetUserId } },
    });
    if (!target) throw new AppError('NOT_FOUND', 'Target not in team');
    if (target.role === role) return target;
    if (role !== TeamRole.OWNER) {
      // Allow only one owner at a time
      return this.prisma.teamMember.update({
        where: { teamId_userId: { teamId, userId: targetUserId } },
        data: { role },
      });
    }
    // Transfer ownership atomically
    return this.prisma.$transaction(async (tx) => {
      await tx.teamMember.update({
        where: { teamId_userId: { teamId, userId: actorId } },
        data: { role: TeamRole.ADMIN },
      });
      return tx.teamMember.update({
        where: { teamId_userId: { teamId, userId: targetUserId } },
        data: { role: TeamRole.OWNER },
      });
    });
  }
}
