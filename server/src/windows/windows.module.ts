import { Global, Module } from '@nestjs/common';
import { WindowsController } from './windows.controller';
import { WindowsService } from './windows.service';

@Global()
@Module({
  controllers: [WindowsController],
  providers: [WindowsService],
  exports: [WindowsService],
})
export class WindowsModule {}
