import { Global, Module } from '@nestjs/common';
import { UnlockRequestsController } from './unlock-requests.controller';
import { UnlockRequestsService } from './unlock-requests.service';

@Global()
@Module({
  controllers: [UnlockRequestsController],
  providers: [UnlockRequestsService],
  exports: [UnlockRequestsService],
})
export class UnlockRequestsModule {}
