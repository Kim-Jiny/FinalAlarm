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
import { MissionsService } from './missions.service';
import { CreateMissionDto, UpdateMissionDto } from './dto';

@Controller('missions')
export class MissionsController {
  constructor(private readonly missions: MissionsService) {}

  @Get()
  list(@CurrentUserId() userId: string) {
    return this.missions.list(userId);
  }

  @Post()
  create(@CurrentUserId() userId: string, @Body() dto: CreateMissionDto) {
    return this.missions.create(userId, dto);
  }

  @Get(':id')
  get(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.missions.get(userId, id);
  }

  @Patch(':id')
  update(
    @CurrentUserId() userId: string,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateMissionDto,
  ) {
    return this.missions.update(userId, id, dto);
  }

  @Delete(':id')
  @HttpCode(204)
  async remove(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.missions.remove(userId, id);
  }
}
