import { IsEnum, IsOptional, IsString, MinLength } from 'class-validator';
import { TeamRole } from '@prisma/client';

export class CreateTeamDto {
  @IsString()
  @MinLength(1)
  name!: string;
}

export class UpdateTeamDto {
  @IsString()
  @MinLength(1)
  @IsOptional()
  name?: string;
}

export class ChangeRoleDto {
  @IsEnum(TeamRole)
  role!: TeamRole;
}
