import {
  IsBoolean,
  IsDateString,
  IsEnum,
  IsInt,
  IsObject,
  IsOptional,
  IsUUID,
  Max,
  Min,
} from 'class-validator';
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

  // 알람 끄는 순간의 디바이스 상태 — 팀원이 "무음 상태로 잠들었구나" 확인용
  @IsInt()
  @Min(0)
  @Max(100)
  @IsOptional()
  volumePct?: number;

  @IsBoolean()
  @IsOptional()
  dnd?: boolean;
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

  // 알람 울리는 시점 디바이스 상태
  @IsInt()
  @Min(0)
  @Max(100)
  @IsOptional()
  volumePctAtTrigger?: number;

  @IsBoolean()
  @IsOptional()
  dndAtTrigger?: boolean;

  // 끈 후에야 서버에 도달하는 경우 (reconcile) dismiss 시점 상태도 같이
  @IsInt()
  @Min(0)
  @Max(100)
  @IsOptional()
  volumePctAtDismiss?: number;

  @IsBoolean()
  @IsOptional()
  dndAtDismiss?: boolean;
}

// 알람 울리는 도중 디바이스 상태 라이브 전송
export class HeartbeatDto {
  @IsInt()
  @Min(0)
  @Max(100)
  volumePct!: number;

  @IsBoolean()
  @IsOptional()
  dnd?: boolean;
}
