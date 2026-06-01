import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ThrottlerModule } from '@nestjs/throttler';
import { PrismaModule } from './prisma/prisma.module';
import { AuthModule } from './auth/auth.module';
import { MeModule } from './me/me.module';
import { TeamsModule } from './teams/teams.module';
import { InvitesModule } from './invites/invites.module';
import { MissionsModule } from './missions/missions.module';
import { PushTokensModule } from './push-tokens/push-tokens.module';
import { AlarmsModule } from './alarms/alarms.module';
import { WindowsModule } from './windows/windows.module';
import { EventsModule } from './events/events.module';
import { UnlockRequestsModule } from './unlock-requests/unlock-requests.module';
import { PushAlarmModule } from './push-alarm/push-alarm.module';
import { FcmModule } from './fcm/fcm.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    ThrottlerModule.forRoot([{ ttl: 60_000, limit: 120 }]),
    PrismaModule,
    FcmModule,
    AuthModule,
    MeModule,
    TeamsModule,
    InvitesModule,
    MissionsModule,
    PushTokensModule,
    AlarmsModule,
    WindowsModule,
    EventsModule,
    UnlockRequestsModule,
    PushAlarmModule,
  ],
})
export class AppModule {}
