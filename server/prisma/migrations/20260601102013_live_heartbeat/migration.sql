-- AlterTable
ALTER TABLE "alarm_events" ADD COLUMN     "last_seen_at" TIMESTAMPTZ,
ADD COLUMN     "live_dnd" BOOLEAN,
ADD COLUMN     "live_volume_pct" INTEGER;
