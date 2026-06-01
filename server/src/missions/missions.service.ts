import { Injectable } from '@nestjs/common';
import { MissionType, Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';
import { CreateMissionDto, UpdateMissionDto } from './dto';

@Injectable()
export class MissionsService {
  constructor(private readonly prisma: PrismaService) {}

  validateConfig(type: MissionType, config: Record<string, unknown>): void {
    switch (type) {
      case MissionType.MATH: {
        const { difficulty, questionCount } = config as {
          difficulty?: string;
          questionCount?: number;
        };
        if (!['easy', 'medium', 'hard'].includes(difficulty ?? '')) {
          throw new AppError('VALIDATION_ERROR', 'difficulty must be easy|medium|hard');
        }
        if (!Number.isInteger(questionCount) || (questionCount as number) < 1 || (questionCount as number) > 20) {
          throw new AppError('VALIDATION_ERROR', 'questionCount must be 1-20');
        }
        break;
      }
      case MissionType.PHOTO: {
        const { mode, expectedCode } = config as { mode?: string; expectedCode?: string };
        if (!['REFERENCE_IMAGE', 'QR', 'BARCODE'].includes(mode ?? '')) {
          throw new AppError('VALIDATION_ERROR', 'mode must be REFERENCE_IMAGE|QR|BARCODE');
        }
        if (expectedCode !== undefined) {
          if (typeof expectedCode !== 'string' || expectedCode.length === 0 || expectedCode.length > 4096) {
            throw new AppError('VALIDATION_ERROR', 'expectedCode must be 1-4096 char string');
          }
          // QR/BARCODE: 기대 raw value 문자열
          // REFERENCE_IMAGE: 16자 hex aHash (예: "ff1c3b0080404040")
          if (mode === 'REFERENCE_IMAGE' && !/^[0-9a-fA-F]{16}$/.test(expectedCode)) {
            throw new AppError('VALIDATION_ERROR',
              'REFERENCE_IMAGE expectedCode must be 16-char hex (aHash)');
          }
        }
        break;
      }
      case MissionType.SHAKE: {
        const { shakeCount } = config as { shakeCount?: number };
        if (!Number.isInteger(shakeCount) || (shakeCount as number) < 5 || (shakeCount as number) > 200) {
          throw new AppError('VALIDATION_ERROR', 'shakeCount must be 5-200');
        }
        break;
      }
    }
  }

  async list(userId: string) {
    return this.prisma.userMission.findMany({
      where: { userId },
      orderBy: [{ isDefault: 'desc' }, { createdAt: 'desc' }],
    });
  }

  async get(userId: string, id: string) {
    const m = await this.prisma.userMission.findUnique({ where: { id } });
    if (!m || m.userId !== userId) throw new AppError('NOT_FOUND', 'Mission not found');
    return m;
  }

  async create(userId: string, dto: CreateMissionDto) {
    this.validateConfig(dto.type, dto.config);
    return this.prisma.$transaction(async (tx) => {
      if (dto.isDefault) {
        await tx.userMission.updateMany({ where: { userId }, data: { isDefault: false } });
      }
      return tx.userMission.create({
        data: {
          userId,
          type: dto.type,
          name: dto.name,
          config: dto.config as Prisma.InputJsonValue,
          isDefault: dto.isDefault ?? false,
        },
      });
    });
  }

  async update(userId: string, id: string, dto: UpdateMissionDto) {
    const existing = await this.get(userId, id);
    if (dto.config) this.validateConfig(existing.type, dto.config);
    return this.prisma.$transaction(async (tx) => {
      if (dto.isDefault) {
        await tx.userMission.updateMany({ where: { userId }, data: { isDefault: false } });
      }
      return tx.userMission.update({
        where: { id },
        data: {
          name: dto.name,
          config: dto.config as Prisma.InputJsonValue | undefined,
          isDefault: dto.isDefault,
        },
      });
    });
  }

  async remove(userId: string, id: string) {
    await this.get(userId, id); // ownership check
    const inUse = await this.prisma.alarmDefinition.count({ where: { missionId: id } });
    if (inUse > 0) {
      throw new AppError('CONFLICT', `Mission is used by ${inUse} alarm(s)`);
    }
    await this.prisma.userMission.delete({ where: { id } });
  }
}
