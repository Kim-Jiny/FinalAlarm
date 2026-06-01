import { Injectable } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as argon2 from 'argon2';
import * as crypto from 'crypto';
import { PrismaService } from '../prisma/prisma.service';
import { AppError } from '../common/errors/app-error';
import { LoginDto, SignupDto } from './dto';

const ACCESS_TTL = process.env.JWT_ACCESS_TTL ?? '1h';
const REFRESH_TTL_DAYS = 30;

function hashToken(token: string): string {
  return crypto.createHash('sha256').update(token).digest('hex');
}

function ttlToDate(days: number): Date {
  return new Date(Date.now() + days * 24 * 60 * 60 * 1000);
}

@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwt: JwtService,
  ) {}

  async signup(dto: SignupDto) {
    const existing = await this.prisma.user.findUnique({ where: { email: dto.email } });
    if (existing) throw new AppError('CONFLICT', 'Email already in use');

    const passwordHash = await argon2.hash(dto.password);
    const user = await this.prisma.user.create({
      data: {
        email: dto.email,
        passwordHash,
        displayName: dto.displayName,
        timezone: dto.timezone ?? 'Asia/Seoul',
      },
    });
    return this.issueTokens(user.id, user.email);
  }

  async login(dto: LoginDto) {
    const user = await this.prisma.user.findUnique({ where: { email: dto.email } });
    if (!user) throw new AppError('UNAUTHORIZED', 'Invalid credentials');

    const ok = await argon2.verify(user.passwordHash, dto.password);
    if (!ok) throw new AppError('UNAUTHORIZED', 'Invalid credentials');

    return this.issueTokens(user.id, user.email);
  }

  async refresh(refreshToken: string) {
    const tokenHash = hashToken(refreshToken);
    const record = await this.prisma.refreshToken.findUnique({ where: { tokenHash } });
    if (!record || record.revokedAt || record.expiresAt < new Date()) {
      throw new AppError('UNAUTHORIZED', 'Invalid refresh token');
    }
    const user = await this.prisma.user.findUnique({ where: { id: record.userId } });
    if (!user) throw new AppError('UNAUTHORIZED', 'User not found');

    // Rotate: revoke old, issue new
    await this.prisma.refreshToken.update({
      where: { id: record.id },
      data: { revokedAt: new Date() },
    });
    return this.issueTokens(user.id, user.email);
  }

  async logout(refreshToken: string) {
    const tokenHash = hashToken(refreshToken);
    await this.prisma.refreshToken.updateMany({
      where: { tokenHash, revokedAt: null },
      data: { revokedAt: new Date() },
    });
  }

  private async issueTokens(userId: string, email: string) {
    const accessToken = await this.jwt.signAsync(
      { sub: userId, email },
      { secret: process.env.JWT_ACCESS_SECRET, expiresIn: ACCESS_TTL },
    );
    const refreshToken = crypto.randomBytes(48).toString('base64url');
    await this.prisma.refreshToken.create({
      data: {
        userId,
        tokenHash: hashToken(refreshToken),
        expiresAt: ttlToDate(REFRESH_TTL_DAYS),
      },
    });
    return { accessToken, refreshToken };
  }
}
