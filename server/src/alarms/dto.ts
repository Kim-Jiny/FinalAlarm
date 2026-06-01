import {
  IsBoolean,
  IsDateString,
  IsEnum,
  IsInt,
  IsOptional,
  IsString,
  IsUUID,
  Matches,
  Max,
  MaxLength,
  Min,
  MinLength,
} from 'class-validator';
import { AlarmKind, ScheduleType, VibrationPattern } from '@prisma/client';
import { TIME_OF_DAY_REGEX } from '../common/util/time';

export class CreateAlarmDto {
  @IsEnum(AlarmKind)
  kind!: AlarmKind;

  @IsUUID()
  @IsOptional()
  teamId?: string;

  @IsString()
  @MinLength(1)
  @MaxLength(80)
  label!: string;

  @IsString()
  timezone!: string;

  @IsEnum(ScheduleType)
  scheduleType!: ScheduleType;

  @IsDateString()
  @IsOptional()
  oneShotAt?: string;

  @Matches(TIME_OF_DAY_REGEX)
  @IsOptional()
  timeOfDay?: string;

  @IsInt()
  @Min(1)
  @Max(127)
  @IsOptional()
  daysOfWeek?: number;

  @IsString()
  soundUri!: string;

  @IsInt()
  @Min(0)
  @Max(100)
  volume!: number;

  @IsInt()
  @Min(0)
  @Max(300)
  volumeRampSeconds!: number;

  @IsBoolean()
  vibrationEnabled!: boolean;

  @IsEnum(VibrationPattern)
  vibrationPattern!: VibrationPattern;

  @IsBoolean()
  snoozeEnabled!: boolean;

  @IsInt()
  @Min(1)
  @Max(60)
  snoozeMinutes!: number;

  @IsInt()
  @Min(-1)
  @Max(20)
  snoozeMaxCount!: number;

  @IsUUID()
  missionId!: string;
}

export class UpdateAlarmDto {
  @IsString()
  @MinLength(1)
  @MaxLength(80)
  @IsOptional()
  label?: string;

  @IsString()
  @IsOptional()
  timezone?: string;

  @IsEnum(ScheduleType)
  @IsOptional()
  scheduleType?: ScheduleType;

  @IsDateString()
  @IsOptional()
  oneShotAt?: string | null;

  @Matches(TIME_OF_DAY_REGEX)
  @IsOptional()
  timeOfDay?: string | null;

  @IsInt()
  @Min(1)
  @Max(127)
  @IsOptional()
  daysOfWeek?: number | null;

  @IsString()
  @IsOptional()
  soundUri?: string;

  @IsInt()
  @Min(0)
  @Max(100)
  @IsOptional()
  volume?: number;

  @IsInt()
  @Min(0)
  @Max(300)
  @IsOptional()
  volumeRampSeconds?: number;

  @IsBoolean()
  @IsOptional()
  vibrationEnabled?: boolean;

  @IsEnum(VibrationPattern)
  @IsOptional()
  vibrationPattern?: VibrationPattern;

  @IsBoolean()
  @IsOptional()
  snoozeEnabled?: boolean;

  @IsInt()
  @Min(1)
  @Max(60)
  @IsOptional()
  snoozeMinutes?: number;

  @IsInt()
  @Min(-1)
  @Max(20)
  @IsOptional()
  snoozeMaxCount?: number;

  @IsUUID()
  @IsOptional()
  missionId?: string;

  @IsBoolean()
  @IsOptional()
  active?: boolean;
}
