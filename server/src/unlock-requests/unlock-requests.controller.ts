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
import {
  ApiBearerAuth,
  ApiNoContentResponse,
  ApiOkResponse,
  ApiTags,
} from '@nestjs/swagger';
import { UnlockRequestStatus } from '@prisma/client';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { UnlockRequestsService } from './unlock-requests.service';
import { UnlockRequestDto } from '../common/dto/responses';

@ApiTags('unlock-requests')
@ApiBearerAuth('access-token')
@Controller('unlock-requests')
export class UnlockRequestsController {
  constructor(private readonly service: UnlockRequestsService) {}

  @Get('inbox')
  @ApiOkResponse({ type: [UnlockRequestDto] })
  inbox(
    @CurrentUserId() userId: string,
    @Query('teamId', ParseUUIDPipe) teamId: string,
    @Query('status', new ParseEnumPipe(UnlockRequestStatus, { optional: true }))
    status?: UnlockRequestStatus,
  ) {
    return this.service.inbox(userId, teamId, status);
  }

  @Get(':id')
  @ApiOkResponse({ type: UnlockRequestDto })
  get(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.service.get(userId, id);
  }

  @Post(':id/approve')
  @ApiOkResponse({ type: UnlockRequestDto })
  approve(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.service.approve(userId, id);
  }

  @Post(':id/cancel')
  @HttpCode(204)
  @ApiNoContentResponse()
  async cancel(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.service.cancel(userId, id);
  }
}
