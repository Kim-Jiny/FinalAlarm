import { Body, Controller, Post } from '@nestjs/common';
import { ApiBearerAuth, ApiOkResponse, ApiTags } from '@nestjs/swagger';
import { Throttle } from '@nestjs/throttler';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { PushAlarmService } from './push-alarm.service';
import { PushAlarmDto } from './dto';
import { AlarmEventDto } from '../common/dto/responses';

@ApiTags('push-alarm')
@ApiBearerAuth('access-token')
@Controller('push-alarm')
export class PushAlarmController {
  constructor(private readonly pushAlarm: PushAlarmService) {}

  @Post()
  @Throttle({ default: { limit: 1, ttl: 60_000 } })
  @ApiOkResponse({ type: AlarmEventDto })
  push(@CurrentUserId() senderId: string, @Body() dto: PushAlarmDto) {
    return this.pushAlarm.push(senderId, dto);
  }
}
