import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  ParseBoolPipe,
  ParseEnumPipe,
  ParseUUIDPipe,
  Patch,
  Post,
  Query,
} from '@nestjs/common';
import { AlarmKind } from '@prisma/client';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { AlarmsService } from './alarms.service';
import { CreateAlarmDto, UpdateAlarmDto } from './dto';

@Controller('alarms')
export class AlarmsController {
  constructor(private readonly alarms: AlarmsService) {}

  @Get()
  list(
    @CurrentUserId() userId: string,
    @Query('teamId') teamId?: string,
    @Query('kind', new ParseEnumPipe(AlarmKind, { optional: true })) kind?: AlarmKind,
    @Query('active', new ParseBoolPipe({ optional: true })) active?: boolean,
  ) {
    return this.alarms.list(userId, { teamId, kind, active });
  }

  @Post()
  create(@CurrentUserId() userId: string, @Body() dto: CreateAlarmDto) {
    return this.alarms.create(userId, dto);
  }

  @Get(':id')
  get(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    return this.alarms.get(userId, id);
  }

  @Patch(':id')
  update(
    @CurrentUserId() userId: string,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateAlarmDto,
  ) {
    return this.alarms.update(userId, id, dto);
  }

  @Delete(':id')
  @HttpCode(204)
  async remove(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.alarms.remove(userId, id);
  }
}
