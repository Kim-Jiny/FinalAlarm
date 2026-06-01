import {
  Controller,
  Get,
  HttpCode,
  Param,
  ParseEnumPipe,
  ParseUUIDPipe,
  Post,
  Query,
} from '@nestjs/common';
import { UnlockRequestStatus } from '@prisma/client';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { UnlockRequestsService } from './unlock-requests.service';

@Controller('unlock-requests')
export class UnlockRequestsController {
  constructor(private readonly service: UnlockRequestsService) {}

  @Get('inbox')
  inbox(
    @CurrentUserId() userId: string,
    @Query('teamId', ParseUUIDPipe) teamId: string,
    @Query('status', new ParseEnumPipe(UnlockRequestStatus, { optional: true }))
    status?: UnlockRequestStatus,
  ) {
    return this.service.inbox(userId, teamId, status);
  }

  @Get(':id')
  get(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.service.get(userId, id);
  }

  @Post(':id/approve')
  approve(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.service.approve(userId, id);
  }

  @Post(':id/cancel')
  @HttpCode(204)
  async cancel(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.service.cancel(userId, id);
  }
}
