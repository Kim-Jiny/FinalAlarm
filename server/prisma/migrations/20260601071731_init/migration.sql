-- CreateEnum
CREATE TYPE "alarm_team_role" AS ENUM ('OWNER', 'ADMIN', 'MEMBER');

-- CreateEnum
CREATE TYPE "alarm_mission_type" AS ENUM ('MATH', 'PHOTO', 'SHAKE');

-- CreateEnum
CREATE TYPE "alarm_kind" AS ENUM ('TEAM_APPROVAL', 'PERSONAL');

-- CreateEnum
CREATE TYPE "alarm_schedule_type" AS ENUM ('ONE_SHOT', 'RECURRING');

-- CreateEnum
CREATE TYPE "alarm_vibration_pattern" AS ENUM ('SHORT', 'MEDIUM', 'LONG', 'PULSE', 'HEARTBEAT');

-- CreateEnum
CREATE TYPE "alarm_event_state" AS ENUM ('RINGING', 'SNOOZED', 'UNLOCK_REQUESTED', 'UNLOCK_APPROVED', 'DISMISSED', 'EXPIRED');

-- CreateEnum
CREATE TYPE "alarm_unlock_request_status" AS ENUM ('PENDING', 'APPROVED', 'EXPIRED', 'CANCELED');

-- CreateEnum
CREATE TYPE "alarm_device_platform" AS ENUM ('ANDROID', 'IOS');

-- CreateTable
CREATE TABLE "alarm_users" (
    "id" UUID NOT NULL,
    "email" TEXT NOT NULL,
    "password_hash" TEXT NOT NULL,
    "display_name" TEXT NOT NULL,
    "avatar_url" TEXT,
    "timezone" TEXT NOT NULL DEFAULT 'Asia/Seoul',
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL,

    CONSTRAINT "alarm_users_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "alarm_refresh_tokens" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "token_hash" TEXT NOT NULL,
    "expires_at" TIMESTAMPTZ NOT NULL,
    "revoked_at" TIMESTAMPTZ,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "alarm_refresh_tokens_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "alarm_teams" (
    "id" UUID NOT NULL,
    "name" TEXT NOT NULL,
    "created_by" UUID NOT NULL,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL,

    CONSTRAINT "alarm_teams_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "alarm_team_members" (
    "team_id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "role" "alarm_team_role" NOT NULL DEFAULT 'MEMBER',
    "joined_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "alarm_team_members_pkey" PRIMARY KEY ("team_id","user_id")
);

-- CreateTable
CREATE TABLE "alarm_team_invites" (
    "id" UUID NOT NULL,
    "team_id" UUID NOT NULL,
    "code" TEXT NOT NULL,
    "created_by" UUID NOT NULL,
    "expires_at" TIMESTAMPTZ NOT NULL,
    "max_uses" INTEGER,
    "use_count" INTEGER NOT NULL DEFAULT 0,
    "revoked_at" TIMESTAMPTZ,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "alarm_team_invites_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "alarm_user_missions" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "type" "alarm_mission_type" NOT NULL,
    "name" TEXT NOT NULL,
    "config" JSONB NOT NULL,
    "is_default" BOOLEAN NOT NULL DEFAULT false,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL,

    CONSTRAINT "alarm_user_missions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "alarm_definitions" (
    "id" UUID NOT NULL,
    "owner_id" UUID NOT NULL,
    "team_id" UUID,
    "kind" "alarm_kind" NOT NULL,
    "label" TEXT NOT NULL,
    "timezone" TEXT NOT NULL,
    "schedule_type" "alarm_schedule_type" NOT NULL,
    "one_shot_at" TIMESTAMPTZ,
    "time_of_day" TEXT,
    "days_of_week" INTEGER,
    "sound_uri" TEXT NOT NULL,
    "volume" INTEGER NOT NULL DEFAULT 80,
    "volume_ramp_seconds" INTEGER NOT NULL DEFAULT 0,
    "vibration_enabled" BOOLEAN NOT NULL DEFAULT true,
    "vibration_pattern" "alarm_vibration_pattern" NOT NULL DEFAULT 'PULSE',
    "snooze_enabled" BOOLEAN NOT NULL DEFAULT true,
    "snooze_minutes" INTEGER NOT NULL DEFAULT 5,
    "snooze_max_count" INTEGER NOT NULL DEFAULT 3,
    "mission_id" UUID NOT NULL,
    "active" BOOLEAN NOT NULL DEFAULT true,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL,

    CONSTRAINT "alarm_definitions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "alarm_windows" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "team_id" UUID NOT NULL,
    "start_time" TEXT NOT NULL,
    "end_time" TEXT NOT NULL,
    "days_of_week" INTEGER NOT NULL,
    "timezone" TEXT NOT NULL,
    "active" BOOLEAN NOT NULL DEFAULT true,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL,

    CONSTRAINT "alarm_windows_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "alarm_events" (
    "id" UUID NOT NULL,
    "definition_id" UUID,
    "window_id" UUID,
    "target_user_id" UUID NOT NULL,
    "sender_user_id" UUID,
    "team_id" UUID,
    "mission_id" UUID NOT NULL,
    "state" "alarm_event_state" NOT NULL DEFAULT 'RINGING',
    "snooze_count" INTEGER NOT NULL DEFAULT 0,
    "last_snoozed_at" TIMESTAMPTZ,
    "next_ring_at" TIMESTAMPTZ,
    "triggered_at" TIMESTAMPTZ NOT NULL,
    "dismissed_at" TIMESTAMPTZ,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "alarm_events_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "alarm_unlock_requests" (
    "id" UUID NOT NULL,
    "event_id" UUID NOT NULL,
    "requester_id" UUID NOT NULL,
    "team_id" UUID NOT NULL,
    "status" "alarm_unlock_request_status" NOT NULL DEFAULT 'PENDING',
    "approved_by" UUID,
    "approved_at" TIMESTAMPTZ,
    "expires_at" TIMESTAMPTZ NOT NULL,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "alarm_unlock_requests_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "alarm_push_tokens" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "platform" "alarm_device_platform" NOT NULL,
    "token" TEXT NOT NULL,
    "device_id" TEXT NOT NULL,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "last_seen_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "alarm_push_tokens_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "alarm_users_email_key" ON "alarm_users"("email");

-- CreateIndex
CREATE UNIQUE INDEX "alarm_refresh_tokens_token_hash_key" ON "alarm_refresh_tokens"("token_hash");

-- CreateIndex
CREATE INDEX "alarm_refresh_tokens_user_id_idx" ON "alarm_refresh_tokens"("user_id");

-- CreateIndex
CREATE INDEX "alarm_team_members_user_id_idx" ON "alarm_team_members"("user_id");

-- CreateIndex
CREATE UNIQUE INDEX "alarm_team_invites_code_key" ON "alarm_team_invites"("code");

-- CreateIndex
CREATE INDEX "alarm_team_invites_team_id_idx" ON "alarm_team_invites"("team_id");

-- CreateIndex
CREATE INDEX "alarm_user_missions_user_id_idx" ON "alarm_user_missions"("user_id");

-- CreateIndex
CREATE INDEX "alarm_definitions_owner_id_active_idx" ON "alarm_definitions"("owner_id", "active");

-- CreateIndex
CREATE INDEX "alarm_definitions_team_id_active_idx" ON "alarm_definitions"("team_id", "active");

-- CreateIndex
CREATE INDEX "alarm_windows_user_id_active_idx" ON "alarm_windows"("user_id", "active");

-- CreateIndex
CREATE INDEX "alarm_windows_team_id_active_idx" ON "alarm_windows"("team_id", "active");

-- CreateIndex
CREATE INDEX "alarm_events_target_user_id_state_idx" ON "alarm_events"("target_user_id", "state");

-- CreateIndex
CREATE INDEX "alarm_events_state_next_ring_at_idx" ON "alarm_events"("state", "next_ring_at");

-- CreateIndex
CREATE INDEX "alarm_events_triggered_at_idx" ON "alarm_events"("triggered_at");

-- CreateIndex
CREATE INDEX "alarm_unlock_requests_team_id_status_idx" ON "alarm_unlock_requests"("team_id", "status");

-- CreateIndex
CREATE INDEX "alarm_unlock_requests_expires_at_status_idx" ON "alarm_unlock_requests"("expires_at", "status");

-- CreateIndex
CREATE UNIQUE INDEX "alarm_push_tokens_token_key" ON "alarm_push_tokens"("token");

-- CreateIndex
CREATE INDEX "alarm_push_tokens_user_id_idx" ON "alarm_push_tokens"("user_id");

-- CreateIndex
CREATE UNIQUE INDEX "alarm_push_tokens_user_id_device_id_key" ON "alarm_push_tokens"("user_id", "device_id");

-- AddForeignKey
ALTER TABLE "alarm_refresh_tokens" ADD CONSTRAINT "alarm_refresh_tokens_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "alarm_users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_teams" ADD CONSTRAINT "alarm_teams_created_by_fkey" FOREIGN KEY ("created_by") REFERENCES "alarm_users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_team_members" ADD CONSTRAINT "alarm_team_members_team_id_fkey" FOREIGN KEY ("team_id") REFERENCES "alarm_teams"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_team_members" ADD CONSTRAINT "alarm_team_members_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "alarm_users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_team_invites" ADD CONSTRAINT "alarm_team_invites_team_id_fkey" FOREIGN KEY ("team_id") REFERENCES "alarm_teams"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_team_invites" ADD CONSTRAINT "alarm_team_invites_created_by_fkey" FOREIGN KEY ("created_by") REFERENCES "alarm_users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_user_missions" ADD CONSTRAINT "alarm_user_missions_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "alarm_users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_definitions" ADD CONSTRAINT "alarm_definitions_owner_id_fkey" FOREIGN KEY ("owner_id") REFERENCES "alarm_users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_definitions" ADD CONSTRAINT "alarm_definitions_team_id_fkey" FOREIGN KEY ("team_id") REFERENCES "alarm_teams"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_definitions" ADD CONSTRAINT "alarm_definitions_mission_id_fkey" FOREIGN KEY ("mission_id") REFERENCES "alarm_user_missions"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_windows" ADD CONSTRAINT "alarm_windows_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "alarm_users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_windows" ADD CONSTRAINT "alarm_windows_team_id_fkey" FOREIGN KEY ("team_id") REFERENCES "alarm_teams"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_events" ADD CONSTRAINT "alarm_events_definition_id_fkey" FOREIGN KEY ("definition_id") REFERENCES "alarm_definitions"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_events" ADD CONSTRAINT "alarm_events_window_id_fkey" FOREIGN KEY ("window_id") REFERENCES "alarm_windows"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_events" ADD CONSTRAINT "alarm_events_target_user_id_fkey" FOREIGN KEY ("target_user_id") REFERENCES "alarm_users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_events" ADD CONSTRAINT "alarm_events_sender_user_id_fkey" FOREIGN KEY ("sender_user_id") REFERENCES "alarm_users"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_events" ADD CONSTRAINT "alarm_events_team_id_fkey" FOREIGN KEY ("team_id") REFERENCES "alarm_teams"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_events" ADD CONSTRAINT "alarm_events_mission_id_fkey" FOREIGN KEY ("mission_id") REFERENCES "alarm_user_missions"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_unlock_requests" ADD CONSTRAINT "alarm_unlock_requests_event_id_fkey" FOREIGN KEY ("event_id") REFERENCES "alarm_events"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_unlock_requests" ADD CONSTRAINT "alarm_unlock_requests_requester_id_fkey" FOREIGN KEY ("requester_id") REFERENCES "alarm_users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_unlock_requests" ADD CONSTRAINT "alarm_unlock_requests_team_id_fkey" FOREIGN KEY ("team_id") REFERENCES "alarm_teams"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_unlock_requests" ADD CONSTRAINT "alarm_unlock_requests_approved_by_fkey" FOREIGN KEY ("approved_by") REFERENCES "alarm_users"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_push_tokens" ADD CONSTRAINT "alarm_push_tokens_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "alarm_users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
