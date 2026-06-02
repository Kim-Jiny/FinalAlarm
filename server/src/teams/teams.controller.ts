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
import {
  ApiBearerAuth,
  ApiNoContentResponse,
  ApiOkResponse,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { TeamsService } from './teams.service';
import { ChangeRoleDto, CreateTeamDto, UpdateTeamDto } from './dto';
import { TeamDto, TeamMemberDto, TeamSummaryDto } from '../common/dto/responses';

@ApiTags('teams')
@ApiBearerAuth('access-token')
@Controller('teams')
export class TeamsController {
  constructor(private readonly teams: TeamsService) {}

  @Get()
  @ApiOkResponse({ type: [TeamSummaryDto] })
  list(@CurrentUserId() userId: string) {
    return this.teams.listMyTeams(userId);
  }

  @Post()
  @ApiOkResponse({ type: TeamDto })
  create(@CurrentUserId() userId: string, @Body() dto: CreateTeamDto) {
    return this.teams.createTeam(userId, dto.name);
  }

  @Get(':id')
  @ApiOkResponse({ type: TeamDto })
  get(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.teams.getTeam(userId, id);
  }

  @Patch(':id')
  @ApiOkResponse({ type: TeamDto })
  update(
    @CurrentUserId() userId: string,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateTeamDto,
  ) {
    return this.teams.updateTeam(userId, id, dto.name);
  }

  @Delete(':id')
  @HttpCode(204)
  @ApiNoContentResponse()
  async remove(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.teams.deleteTeam(userId, id);
  }

  @Delete(':id/members/me')
  @HttpCode(204)
  @ApiNoContentResponse()
  async leave(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.teams.leaveTeam(userId, id);
  }

  @Delete(':id/members/:userId')
  @HttpCode(204)
  @ApiNoContentResponse()
  async kick(
    @CurrentUserId() actorId: string,
    @Param('id', ParseUUIDPipe) teamId: string,
    @Param('userId', ParseUUIDPipe) targetUserId: string,
  ) {
    await this.teams.kickMember(actorId, teamId, targetUserId);
  }

  @Patch(':id/members/:userId')
  @ApiOkResponse({ type: TeamMemberDto })
  changeRole(
    @CurrentUserId() actorId: string,
    @Param('id', ParseUUIDPipe) teamId: string,
    @Param('userId', ParseUUIDPipe) targetUserId: string,
    @Body() dto: ChangeRoleDto,
  ) {
    return this.teams.changeRole(actorId, teamId, targetUserId, dto.role);
  }
}
