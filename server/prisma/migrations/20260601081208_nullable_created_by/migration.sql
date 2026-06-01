-- DropForeignKey
ALTER TABLE "alarm_team_invites" DROP CONSTRAINT "alarm_team_invites_created_by_fkey";

-- DropForeignKey
ALTER TABLE "alarm_teams" DROP CONSTRAINT "alarm_teams_created_by_fkey";

-- AlterTable
ALTER TABLE "alarm_team_invites" ALTER COLUMN "created_by" DROP NOT NULL;

-- AlterTable
ALTER TABLE "alarm_teams" ALTER COLUMN "created_by" DROP NOT NULL;

-- AddForeignKey
ALTER TABLE "alarm_teams" ADD CONSTRAINT "alarm_teams_created_by_fkey" FOREIGN KEY ("created_by") REFERENCES "alarm_users"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "alarm_team_invites" ADD CONSTRAINT "alarm_team_invites_created_by_fkey" FOREIGN KEY ("created_by") REFERENCES "alarm_users"("id") ON DELETE SET NULL ON UPDATE CASCADE;
