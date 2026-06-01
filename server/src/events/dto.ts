import { IsDateString, IsEnum, IsObject, IsOptional, IsUUID } from 'class-validator';
import { MissionType } from '@prisma/client';

export class DismissDto {
  @IsObject()
  missionProof!: {
    type: MissionType;
    // type-specific fields, validated in service
    [k: string]: unknown;
  };

  @IsEnum(MissionType)
  @IsOptional()
  expectedType?: MissionType;
}

// 클라이언트의 로컬 AlarmManager가 알람을 발사했을 때 서버에 보고
export class CreateEventDto {
  @IsUUID()
  definitionId!: string;

  @IsDateString()
  @IsOptional()
  triggeredAt?: string;

  // 오프라인 reconcile용: 클라가 끈 후에야 서버에 도달하는 경우 'DISMISSED'로 생성
  @IsEnum(['RINGING', 'DISMISSED'] as any, { each: false })
  @IsOptional()
  initialState?: 'RINGING' | 'DISMISSED';

  @IsDateString()
  @IsOptional()
  dismissedAt?: string;
}
