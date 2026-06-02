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
import { MissionsService } from './missions.service';
import { CreateMissionDto, UpdateMissionDto } from './dto';
import { MissionDto } from '../common/dto/responses';

@ApiTags('missions')
@ApiBearerAuth('access-token')
@Controller('missions')
export class MissionsController {
  constructor(private readonly missions: MissionsService) {}

  @Get()
  @ApiOkResponse({ type: [MissionDto] })
  list(@CurrentUserId() userId: string) {
    return this.missions.list(userId);
  }

  @Post()
  @ApiOkResponse({ type: MissionDto })
  create(@CurrentUserId() userId: string, @Body() dto: CreateMissionDto) {
    return this.missions.create(userId, dto);
  }

  @Get(':id')
  @ApiOkResponse({ type: MissionDto })
  get(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.missions.get(userId, id);
  }

  @Patch(':id')
  @ApiOkResponse({ type: MissionDto })
  update(
    @CurrentUserId() userId: string,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateMissionDto,
  ) {
    return this.missions.update(userId, id, dto);
  }

  @Delete(':id')
  @HttpCode(204)
  @ApiNoContentResponse()
  async remove(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.missions.remove(userId, id);
  }
}
