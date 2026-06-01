import { Module } from '@nestjs/common';
import { PushAlarmController } from './push-alarm.controller';
import { PushAlarmService } from './push-alarm.service';

@Module({
  controllers: [PushAlarmController],
  providers: [PushAlarmService],
})
export class PushAlarmModule {}
