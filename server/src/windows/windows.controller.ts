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
import {
  ApiBearerAuth,
  ApiNoContentResponse,
  ApiOkResponse,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { WindowsService } from './windows.service';
import { CreateWindowDto, UpdateWindowDto } from './dto';
import { AlarmWindowDto } from '../common/dto/responses';

@ApiTags('alarm-windows')
@ApiBearerAuth('access-token')
@Controller('alarm-windows')
export class WindowsController {
  constructor(private readonly windows: WindowsService) {}

  @Get()
  @ApiOkResponse({ type: [AlarmWindowDto] })
  list(
    @CurrentUserId() userId: string,
    @Query('teamId') teamId?: string,
    @Query('userId') userQ?: string,
  ) {
    if (teamId) return this.windows.listByTeam(userId, teamId);
    return this.windows.listMine(userId);
  }

  @Post()
  @ApiOkResponse({ type: AlarmWindowDto })
  create(@CurrentUserId() userId: string, @Body() dto: CreateWindowDto) {
    return this.windows.create(userId, dto);
  }

  @Patch(':id')
  @ApiOkResponse({ type: AlarmWindowDto })
  update(
    @CurrentUserId() userId: string,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateWindowDto,
  ) {
    return this.windows.update(userId, id, dto);
  }

  @Delete(':id')
  @HttpCode(204)
  @ApiNoContentResponse()
  async remove(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.windows.remove(userId, id);
  }
}
