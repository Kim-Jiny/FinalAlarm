import {
  Body,
  Controller,
  Delete,
  HttpCode,
  Param,
  ParseUUIDPipe,
  Post,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiNoContentResponse,
  ApiOkResponse,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUserId } from '../common/decorators/current-user.decorator';
import { PushTokensService } from './push-tokens.service';
import { RegisterPushTokenDto } from './dto';
import { PushTokenDto } from '../common/dto/responses';

@ApiTags('push-tokens')
@ApiBearerAuth('access-token')
@Controller('push-tokens')
export class PushTokensController {
  constructor(private readonly tokens: PushTokensService) {}

  @Post()
  @ApiOkResponse({ type: PushTokenDto })
  register(@CurrentUserId() userId: string, @Body() dto: RegisterPushTokenDto) {
    return this.tokens.upsert(userId, dto.platform, dto.token, dto.deviceId);
  }

  @Delete(':id')
  @HttpCode(204)
  @ApiNoContentResponse()
  async remove(@CurrentUserId() userId: string, @Param('id', ParseUUIDPipe) id: string) {
    await this.tokens.remove(userId, id);
  }
}
