import { Body, Controller, Delete, Get, HttpCode, Patch, Post } from '@nestjs/common';
import { IsOptional, IsString, IsUrl, MinLength } from 'class-validator';
import * as argon2 from 'argon2';
import { PrismaService } from '../prisma/prisma.service';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { AppError } from '../common/errors/app-error';

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

@Controller('me')
export class MeController {
  constructor(private readonly prisma: PrismaService) {}

  @Get()
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
  async deleteMe(@CurrentUserId() userId: string) {
    await this.prisma.user.delete({ where: { id: userId } });
  }
}
