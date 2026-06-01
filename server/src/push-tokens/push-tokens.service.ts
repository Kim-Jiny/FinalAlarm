import { Injectable } from '@nestjs/common';
import { DevicePlatform } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';

@Injectable()
export class PushTokensService {
  constructor(private readonly prisma: PrismaService) {}

  async upsert(
    userId: string,
    platform: DevicePlatform,
    token: string,
    deviceId: string,
  ) {
    // 같은 토큰이 다른 user에게 묶여 있으면 그 쪽 해제 (기기 이전 사용자 처리)
    await this.prisma.pushToken.deleteMany({
      where: { token, NOT: { userId } },
    });
    return this.prisma.pushToken.upsert({
      where: { userId_deviceId: { userId, deviceId } },
      create: { userId, platform, token, deviceId },
      update: { token, platform, lastSeenAt: new Date() },
    });
  }

  async remove(userId: string, id: string) {
    const t = await this.prisma.pushToken.findUnique({ where: { id } });
    if (!t || t.userId !== userId) throw new AppError('NOT_FOUND', 'Token not found');
    await this.prisma.pushToken.delete({ where: { id } });
  }

  async listForUser(userId: string) {
    return this.prisma.pushToken.findMany({ where: { userId } });
  }

  async invalidate(token: string) {
    await this.prisma.pushToken.deleteMany({ where: { token } });
  }
}
