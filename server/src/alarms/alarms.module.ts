import { Global, Module } from '@nestjs/common';
import { AlarmsController } from './alarms.controller';
import { AlarmsService } from './alarms.service';

@Global()
@Module({
  controllers: [AlarmsController],
  providers: [AlarmsService],
  exports: [AlarmsService],
})
export class AlarmsModule {}
