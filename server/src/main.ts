import { NestFactory } from '@nestjs/core';
import { Logger, ValidationPipe } from '@nestjs/common';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import helmet from 'helmet';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { AppModule } from './app.module';
import { AllExceptionsFilter } from './common/filters/all-exceptions.filter';

function requireEnv(name: string): void {
  const v = process.env[name];
  if (!v || v.trim().length === 0) {
    throw new Error(`Required env var ${name} is missing or empty`);
  }
}

function buildSwaggerConfig() {
  return new DocumentBuilder()
    .setTitle('FinalAlarm API')
    .setDescription('팀 기반 알람 앱 백엔드')
    .setVersion('1.0')
    .addBearerAuth({ type: 'http', scheme: 'bearer', bearerFormat: 'JWT' }, 'access-token')
    .addServer('/api/v1')
    .build();
}

async function bootstrap() {
  ['DATABASE_URL', 'JWT_ACCESS_SECRET', 'JWT_REFRESH_SECRET'].forEach(requireEnv);

  const app = await NestFactory.create(AppModule, { bufferLogs: true });
  const logger = new Logger('Bootstrap');

  app.use(helmet({ contentSecurityPolicy: false, crossOriginEmbedderPolicy: false }));
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

  // ---- Swagger / OpenAPI ----
  const config = buildSwaggerConfig();
  const document = SwaggerModule.createDocument(app, config);

  // export-only 모드: openapi.json 파일로 쓰고 종료. CI/codegen용.
  if (process.env.OPENAPI_EXPORT === '1') {
    const outPath = path.resolve(process.cwd(), 'openapi.json');
    fs.writeFileSync(outPath, JSON.stringify(document, null, 2));
    logger.log(`OpenAPI spec exported to ${outPath}`);
    await app.close();
    process.exit(0);
  }

  SwaggerModule.setup('api/v1/docs', app, document, {
    swaggerOptions: { persistAuthorization: true },
  });

  const port = Number(process.env.PORT ?? 3000);
  await app.listen(port);
  logger.log(`FinalAlarm API listening on ${port}`);
  logger.log(`API docs at http://localhost:${port}/api/v1/docs`);
}

bootstrap();
