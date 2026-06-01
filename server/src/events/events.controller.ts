import {
  Body,
  Controller,
  Get,
  Param,
  ParseIntPipe,
  ParseUUIDPipe,
  Post,
  Query,
} from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { EventsService } from './events.service';
import { CreateEventDto, DismissDto } from './dto';

@Controller('alarm-events')
export class EventsController {
  constructor(private readonly events: EventsService) {}

  @Get()
  listActive(@CurrentUserId() userId: string) {
    return this.events.listActive(userId);
  }

  // 클라 로컬 알람이 발사됐을 때 호출
  @Post()
  create(@CurrentUserId() userId: string, @Body() dto: CreateEventDto) {
    return this.events.createFromDefinition(userId, dto.definitionId, dto.triggeredAt);
  }

  @Get('history')
  history(
    @CurrentUserId() userId: string,
    @Query('from') from?: string,
    @Query('to') to?: string,
    @Query('limit', new ParseIntPipe({ optional: true })) limit?: number,
  ) {
    return this.events.history(userId, from, to, limit);
  }

  @Get(':id')
  get(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.events.get(userId, id);
  }

  @Post(':id/snooze')
  snooze(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.events.snooze(userId, id);
  }

  @Post(':id/dismiss')
  dismiss(
    @CurrentUserId() userId: string,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: DismissDto,
  ) {
    return this.events.dismiss(userId, id, dto.missionProof);
  }

  @Post(':id/unlock-request')
  @Throttle({ default: { limit: 1, ttl: 60_000 } })
  unlockRequest(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.events.requestUnlock(userId, id);
  }
}
