import { Body, Controller, Delete, Get, HttpCode, Patch, Post } from '@nestjs/common';
import { ApiBearerAuth, ApiNoContentResponse, ApiOkResponse, ApiTags } from '@nestjs/swagger';
import { IsOptional, IsString, IsUrl, MinLength } from 'class-validator';
import { TeamRole } from '@prisma/client';
import * as argon2 from 'argon2';
import { PrismaService } from '../prisma/prisma.service';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { AppError } from '../common/errors/app-error';
import { UserDto } from '../common/dto/responses';

class UpdateMeDto {
  @IsString()
  @MinLength(1)
  @IsOptional()
  displayName?: string;

  @IsUrl()
  @IsOptional()
  avatarUrl?: string;

  @IsString()
  @IsOptional()
  timezone?: string;
}

class ChangePasswordDto {
  @IsString()
  @MinLength(1)
  currentPassword!: string;

  @IsString()
  @MinLength(8)
  newPassword!: string;
}

@ApiTags('me')
@ApiBearerAuth('access-token')
@Controller('me')
export class MeController {
  constructor(private readonly prisma: PrismaService) {}

  @Get()
  @ApiOkResponse({ type: UserDto })
  async getMe(@CurrentUserId() userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        email: true,
        displayName: true,
        avatarUrl: true,
        timezone: true,
        createdAt: true,
      },
    });
    if (!user) throw new AppError('NOT_FOUND', 'User not found');
    return user;
  }

  @Patch()
  @ApiOkResponse({ type: UserDto })
  async updateMe(@CurrentUserId() userId: string, @Body() dto: UpdateMeDto) {
    return this.prisma.user.update({
      where: { id: userId },
      data: dto,
      select: {
        id: true,
        email: true,
        displayName: true,
        avatarUrl: true,
        timezone: true,
      },
    });
  }

  @Post('password')
  @HttpCode(204)
  @ApiNoContentResponse()
  async changePassword(@CurrentUserId() userId: string, @Body() dto: ChangePasswordDto) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new AppError('NOT_FOUND', 'User not found');
    const ok = await argon2.verify(user.passwordHash, dto.currentPassword);
    if (!ok) throw new AppError('UNAUTHORIZED', 'Current password is incorrect');
    if (dto.currentPassword === dto.newPassword) {
      throw new AppError('VALIDATION_ERROR', '새 비밀번호가 기존과 같습니다');
    }
    const newHash = await argon2.hash(dto.newPassword);
    await this.prisma.$transaction(async (tx) => {
      await tx.user.update({ where: { id: userId }, data: { passwordHash: newHash } });
      // 모든 refresh token revoke (다른 기기 강제 로그아웃)
      await tx.refreshToken.updateMany({
        where: { userId, revokedAt: null },
        data: { revokedAt: new Date() },
      });
    });
  }

  @Delete()
  @HttpCode(204)
  @ApiNoContentResponse()
  async deleteMe(@CurrentUserId() userId: string) {
    await this.prisma.$transaction(async (tx) => {
      // 본인이 OWNER인 팀들 처리
      const ownedTeams = await tx.teamMember.findMany({
        where: { userId, role: TeamRole.OWNER },
        select: { teamId: true },
      });

      for (const { teamId } of ownedTeams) {
        // 가장 먼저 가입한 ADMIN 찾아서 오너 이양
        const successor = await tx.teamMember.findFirst({
          where: { teamId, role: TeamRole.ADMIN, NOT: { userId } },
          orderBy: { joinedAt: 'asc' },
        });

        if (successor) {
          await tx.teamMember.update({
            where: { teamId_userId: { teamId, userId: successor.userId } },
            data: { role: TeamRole.OWNER },
          });
        } else {
          // 이양할 ADMIN 없음 → 팀 삭제 (멤버·초대·알람·시간대·이벤트 Cascade)
          await tx.team.delete({ where: { id: teamId } });
        }
      }

      // 본인 삭제 (남은 관계는 Cascade / SetNull로 정리)
      await tx.user.delete({ where: { id: userId } });
    });
  }
}
