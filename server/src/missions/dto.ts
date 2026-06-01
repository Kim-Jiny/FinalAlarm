import { IsBoolean, IsEnum, IsObject, IsOptional, IsString, MinLength } from 'class-validator';
import { MissionType } from '@prisma/client';

export class CreateMissionDto {
  @IsEnum(MissionType)
  type!: MissionType;

  @IsString()
  @MinLength(1)
  name!: string;

  @IsObject()
  config!: Record<string, unknown>;

  @IsBoolean()
  @IsOptional()
  isDefault?: boolean;
}

export class UpdateMissionDto {
  @IsString()
  @MinLength(1)
  @IsOptional()
  name?: string;

  @IsObject()
  @IsOptional()
  config?: Record<string, unknown>;

  @IsBoolean()
  @IsOptional()
  isDefault?: boolean;
}
