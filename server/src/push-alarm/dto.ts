import { IsOptional, IsString, IsUUID, MaxLength } from 'class-validator';

export class PushAlarmDto {
  @IsUUID()
  targetUserId!: string;

  @IsUUID()
  teamId!: string;

  @IsString()
  @MaxLength(80)
  @IsOptional()
  label?: string;
}
