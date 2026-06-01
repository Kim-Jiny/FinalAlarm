import { Injectable } from '@nestjs/common';
import { TeamRole } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';

@Injectable()
export class MembershipHelper {
  constructor(private readonly prisma: PrismaService) {}

  async requireMember(userId: string, teamId: string): Promise<TeamRole> {
    const m = await this.prisma.teamMember.findUnique({
      where: { teamId_userId: { teamId, userId } },
    });
    if (!m) throw new AppError('TEAM_NOT_MEMBER', 'Not a member of this team');
    return m.role;
  }

  async requireRole(
    userId: string,
    teamId: string,
    allowed: TeamRole[],
  ): Promise<TeamRole> {
    const role = await this.requireMember(userId, teamId);
    if (!allowed.includes(role)) {
      throw new AppError('FORBIDDEN', 'Insufficient role');
    }
    return role;
  }
}
