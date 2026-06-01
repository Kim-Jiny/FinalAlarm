import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  Logger,
} from '@nestjs/common';
import { Request, Response } from 'express';
import { AppError } from '../errors/app-error';

@Catch()
export class AllExceptionsFilter implements ExceptionFilter {
  private readonly logger = new Logger(AllExceptionsFilter.name);

  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();
    const request = ctx.getRequest<Request>();

    if (exception instanceof AppError) {
      response.status(exception.status).json({
        error: { code: exception.code, message: exception.message, details: exception.details },
      });
      return;
    }

    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      const body = exception.getResponse();
      const message = typeof body === 'string' ? body : (body as any).message ?? exception.message;
      const code = status === 401 ? 'UNAUTHORIZED' : status === 403 ? 'FORBIDDEN' : 'VALIDATION_ERROR';
      response.status(status).json({ error: { code, message } });
      return;
    }

    this.logger.error(`Unhandled error on ${request.method} ${request.url}`, exception as Error);
    response.status(500).json({ error: { code: 'INTERNAL', message: 'Internal server error' } });
  }
}
