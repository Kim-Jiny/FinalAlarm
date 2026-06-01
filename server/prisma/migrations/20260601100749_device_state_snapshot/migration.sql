-- AlterTable
ALTER TABLE "alarm_events" ADD COLUMN     "dnd_at_dismiss" BOOLEAN,
ADD COLUMN     "dnd_at_trigger" BOOLEAN,
ADD COLUMN     "volume_pct_at_dismiss" INTEGER,
ADD COLUMN     "volume_pct_at_trigger" INTEGER;
