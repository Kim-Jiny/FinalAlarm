import { IsInt, IsOptional, Max, Min } from 'class-validator';

export class CreateInviteDto {
  @IsInt()
  @Min(1)
  @Max(90)
  @IsOptional()
  expiresInDays?: number;

  @IsInt()
  @Min(1)
  @Max(1000)
  @IsOptional()
  maxUses?: number;
}
