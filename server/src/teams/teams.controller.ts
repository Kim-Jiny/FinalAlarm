import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  ParseUUIDPipe,
  Patch,
  Post,
} from '@nestjs/common';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { TeamsService } from './teams.service';
import { ChangeRoleDto, CreateTeamDto, UpdateTeamDto } from './dto';

@Controller('teams')
export class TeamsController {
  constructor(private readonly teams: TeamsService) {}

  @Get()
  list(@CurrentUserId() userId: string) {
    return this.teams.listMyTeams(userId);
  }

  @Post()
  create(@CurrentUserId() userId: string, @Body() dto: CreateTeamDto) {
    return this.teams.createTeam(userId, dto.name);
  }

  @Get(':id')
  get(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.teams.getTeam(userId, id);
  }

  @Patch(':id')
  update(
    @CurrentUserId() userId: string,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateTeamDto,
  ) {
    return this.teams.updateTeam(userId, id, dto.name);
  }

  @Delete(':id')
  @HttpCode(204)
  async remove(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.teams.deleteTeam(userId, id);
  }

  @Delete(':id/members/me')
  @HttpCode(204)
  async leave(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.teams.leaveTeam(userId, id);
  }

  @Delete(':id/members/:userId')
  @HttpCode(204)
  async kick(
    @CurrentUserId() actorId: string,
    @Param('id', ParseUUIDPipe) teamId: string,
    @Param('userId', ParseUUIDPipe) targetUserId: string,
  ) {
    await this.teams.kickMember(actorId, teamId, targetUserId);
  }

  @Patch(':id/members/:userId')
  changeRole(
    @CurrentUserId() actorId: string,
    @Param('id', ParseUUIDPipe) teamId: string,
    @Param('userId', ParseUUIDPipe) targetUserId: string,
    @Body() dto: ChangeRoleDto,
  ) {
    return this.teams.changeRole(actorId, teamId, targetUserId, dto.role);
  }
}
