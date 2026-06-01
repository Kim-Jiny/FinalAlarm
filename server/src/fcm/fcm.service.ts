import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import * as admin from 'firebase-admin';
import * as fs from 'node:fs';
import { PushTokensService } from '../push-tokens/push-tokens.service';

type DataPayload = Record<string, string>;

// 영구 실패로 간주하고 토큰을 무효화할 에러 코드 (재시도 무의미)
const PERMANENT_FCM_ERRORS = new Set([
  'messaging/registration-token-not-registered',
  'messaging/invalid-argument',
  'messaging/invalid-registration-token',
]);

// 총 3회 시도 (최초 + 2회 재시도)
const RETRY_DELAYS_MS = [1_000, 3_000];

function sleep(ms: number) {
  return new Promise<void>((resolve) => setTimeout(resolve, ms));
}

@Injectable()
export class FcmService implements OnModuleInit {
  private readonly logger = new Logger(FcmService.name);
  private app?: admin.app.App;

  constructor(private readonly pushTokens: PushTokensService) {}

  onModuleInit() {
    const path = process.env.FCM_SERVICE_ACCOUNT_PATH;
    if (!path) {
      this.logger.warn('FCM_SERVICE_ACCOUNT_PATH not set — FCM disabled');
      return;
    }
    try {
      const json = JSON.parse(fs.readFileSync(path, 'utf-8'));
      this.app = admin.initializeApp({
        credential: admin.credential.cert(json),
      });
      this.logger.log('FCM initialized');
    } catch (e) {
      this.logger.error('Failed to initialize FCM', e as Error);
    }
  }

  async sendToUser(userId: string, payload: DataPayload): Promise<void> {
    if (!this.app) {
      this.logger.warn(`FCM disabled, would send ${payload.type} to ${userId}`);
      return;
    }
    const tokens = await this.pushTokens.listForUser(userId);
    if (tokens.length === 0) return;

    const messaging = admin.messaging(this.app);
    await Promise.all(
      tokens.map((t) => this.sendOneWithRetry(messaging, t, payload)),
    );
  }

  private async sendOneWithRetry(
    messaging: admin.messaging.Messaging,
    tokenRow: { id: string; token: string },
    payload: DataPayload,
  ): Promise<void> {
    let attempt = 0;
    while (true) {
      try {
        await messaging.send({
          token: tokenRow.token,
          data: payload,
          android: { priority: 'high', ttl: 0 },
          apns: {
            headers: { 'apns-priority': '10', 'apns-push-type': 'alert' },
            payload: { aps: { 'content-available': 1 } },
          },
        });
        return;
      } catch (err) {
        const code = (err as { code?: string })?.code;
        if (code && PERMANENT_FCM_ERRORS.has(code)) {
          this.logger.warn(`Invalidating token ${tokenRow.id} (${code})`);
          await this.pushTokens.invalidate(tokenRow.token);
          return;
        }
        if (attempt >= RETRY_DELAYS_MS.length) {
          this.logger.error(
            `FCM send failed for ${tokenRow.id} after ${attempt + 1} attempts`,
            err as any,
          );
          return;
        }
        await sleep(RETRY_DELAYS_MS[attempt]);
        attempt += 1;
      }
    }
  }
}
