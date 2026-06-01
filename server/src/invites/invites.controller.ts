import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  ParseUUIDPipe,
  Post,
} from '@nestjs/common';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { Public } from '../common/guards/jwt-auth.guard';
import { InvitesService } from './invites.service';
import { CreateInviteDto } from './dto';

@Controller()
export class InvitesController {
  constructor(private readonly invites: InvitesService) {}

  @Get('teams/:id/invites')
  list(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.invites.list(userId, id);
  }

  @Post('teams/:id/invites')
  create(
    @CurrentUserId() userId: string,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: CreateInviteDto,
  ) {
    return this.invites.create(userId, id, dto.expiresInDays, dto.maxUses);
  }

  @Delete('teams/:id/invites/:inviteId')
  @HttpCode(204)
  async revoke(
    @CurrentUserId() userId: string,
    @Param('id', ParseUUIDPipe) id: string,
    @Param('inviteId', ParseUUIDPipe) inviteId: string,
  ) {
    await this.invites.revoke(userId, id, inviteId);
  }

  @Public()
  @Get('team-invites/:code/preview')
  preview(@Param('code') code: string) {
    return this.invites.preview(code);
  }

  @Post('team-invites/:code/redeem')
  redeem(@CurrentUserId() userId: string, @Param('code') code: string) {
    return this.invites.redeem(userId, code);
  }
}
