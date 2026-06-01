import { Body, Controller, Delete, Get, HttpCode, Patch } from '@nestjs/common';
import { IsOptional, IsString, IsUrl, MinLength } from 'class-validator';
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

  @Delete()
  @HttpCode(204)
  async deleteMe(@CurrentUserId() userId: string) {
    await this.prisma.user.delete({ where: { id: userId } });
  }
}
