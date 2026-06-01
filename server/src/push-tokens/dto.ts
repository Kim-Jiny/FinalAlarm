import { IsEnum, IsString, MinLength } from 'class-validator';
import { DevicePlatform } from '@prisma/client';

export class RegisterPushTokenDto {
  @IsEnum(DevicePlatform)
  platform!: DevicePlatform;

  @IsString()
  @MinLength(10)
  token!: string;

  @IsString()
  @MinLength(1)
  deviceId!: string;
}
