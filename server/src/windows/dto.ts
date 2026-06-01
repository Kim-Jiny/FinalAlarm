import { IsBoolean, IsInt, IsOptional, IsString, IsUUID, Matches, Max, Min } from 'class-validator';
import { TIME_OF_DAY_REGEX } from '../common/util/time';

export class CreateWindowDto {
  @IsUUID()
  teamId!: string;

  @Matches(TIME_OF_DAY_REGEX)
  startTime!: string;

  @Matches(TIME_OF_DAY_REGEX)
  endTime!: string;

  @IsInt()
  @Min(1)
  @Max(127)
  daysOfWeek!: number;

  @IsString()
  timezone!: string;
}

export class UpdateWindowDto {
  @Matches(TIME_OF_DAY_REGEX)
  @IsOptional()
  startTime?: string;

  @Matches(TIME_OF_DAY_REGEX)
  @IsOptional()
  endTime?: string;

  @IsInt()
  @Min(1)
  @Max(127)
  @IsOptional()
  daysOfWeek?: number;

  @IsString()
  @IsOptional()
  timezone?: string;

  @IsBoolean()
  @IsOptional()
  active?: boolean;
}
