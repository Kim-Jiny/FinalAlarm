import { NestFactory } from '@nestjs/core';
import { Logger, ValidationPipe } from '@nestjs/common';
import helmet from 'helmet';
import { AppModule } from './app.module';
import { AllExceptionsFilter } from './common/filters/all-exceptions.filter';

function requireEnv(name: string): void {
  const v = process.env[name];
  if (!v || v.trim().length === 0) {
    // 부팅 단계에서 명확히 실패하게 — 토큰 위·변조 위험 방지
    throw new Error(`Required env var ${name} is missing or empty`);
  }
}

async function bootstrap() {
  ['DATABASE_URL', 'JWT_ACCESS_SECRET', 'JWT_REFRESH_SECRET'].forEach(requireEnv);

  const app = await NestFactory.create(AppModule, { bufferLogs: true });
  const logger = new Logger('Bootstrap');

  // 보안 헤더 (X-Content-Type-Options, X-Frame-Options 등). 모바일 API라 CSP 등은 불필요.
  app.use(helmet({ contentSecurityPolicy: false, crossOriginEmbedderPolicy: false }));
  // CORS — 모바일 클라만 사용하므로 기본은 닫고, 개발용으로 동일 호스트만 열기
  app.enableCors({ origin: false });

  app.setGlobalPrefix('api/v1');
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );
  app.useGlobalFilters(new AllExceptionsFilter());

  const port = Number(process.env.PORT ?? 3000);
  await app.listen(port);
  logger.log(`FinalAlarm API listening on ${port}`);
}

bootstrap();
