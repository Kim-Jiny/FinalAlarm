import { Global, Module } from '@nestjs/common';
import { TeamsController } from './teams.controller';
import { TeamsService } from './teams.service';
import { MembershipHelper } from './membership.helper';

@Global()
@Module({
  controllers: [TeamsController],
  providers: [TeamsService, MembershipHelper],
  exports: [MembershipHelper],
})
export class TeamsModule {}
