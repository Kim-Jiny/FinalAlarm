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
  Query,
} from '@nestjs/common';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { WindowsService } from './windows.service';
import { CreateWindowDto, UpdateWindowDto } from './dto';

@Controller('alarm-windows')
export class WindowsController {
  constructor(private readonly windows: WindowsService) {}

  @Get()
  list(
    @CurrentUserId() userId: string,
    @Query('teamId') teamId?: string,
    @Query('userId') userQ?: string,
  ) {
    if (teamId) return this.windows.listByTeam(userId, teamId);
    // 기본: 내 windows. ?userId 와 me만 허용 (간단화)
    return this.windows.listMine(userId);
  }

  @Post()
  create(@CurrentUserId() userId: string, @Body() dto: CreateWindowDto) {
    return this.windows.create(userId, dto);
  }

  @Patch(':id')
  update(
    @CurrentUserId() userId: string,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateWindowDto,
  ) {
    return this.windows.update(userId, id, dto);
  }

  @Delete(':id')
  @HttpCode(204)
  async remove(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.windows.remove(userId, id);
  }
}
