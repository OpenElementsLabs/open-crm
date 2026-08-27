-- Rollback for spec 115 / migration V36__move_spring_services_to_dedicated_schema.sql
--
-- Use ONLY when spring-services 1.3.1 has failed in production AFTER V36 was applied.
--
-- Procedure (all three steps, in this order):
--   1. Roll the app image back to the pre-115 version in Coolify (do not let it start yet,
--      or accept that its first start will fail until this script has run).
--   2. Run this script against the production database as the owner role.
--   3. Start the rolled-back image and re-run the smoke checks
--      (login, search, comments, audit log, /admin/backup, MCP).
--
-- Step 3 of this script is not optional: without it the previous image aborts at startup with
-- "Detected applied migration not resolved locally: 36", because its jar carries no V36 file.
--
-- Rehearse this script on the Coolify dev instance before the production deploy.

BEGIN;

-- 1. Move the seven spring-services tables back into the default schema.
--    PostgreSQL moves constraints and indexes with the table; the cross-schema foreign keys from
--    the CRM's own tables (opportunities.owner_id, *_tags.tag_id, *_comments.comment_id,
--    audit_log.user_id, comments.author_id) follow automatically and stay valid.
ALTER TABLE oe_spring_services.users     SET SCHEMA public;
ALTER TABLE oe_spring_services.api_keys  SET SCHEMA public;
ALTER TABLE oe_spring_services.audit_log SET SCHEMA public;
ALTER TABLE oe_spring_services.comments  SET SCHEMA public;
ALTER TABLE oe_spring_services.settings  SET SCHEMA public;
ALTER TABLE oe_spring_services.tags      SET SCHEMA public;
ALTER TABLE oe_spring_services.webhooks  SET SCHEMA public;

-- 2. Drop the now-empty schema. Deliberately RESTRICT (the default): if anything unexpected is
--    still in there, this fails loudly and rolls the whole script back rather than dropping data.
DROP SCHEMA oe_spring_services RESTRICT;

-- 3. Remove the V36 row so Flyway validation passes for the rolled-back image.
DELETE FROM public.flyway_schema_history WHERE version = '36';

COMMIT;

-- Verification after COMMIT — all three queries must return the expected result:
--   SELECT table_name FROM information_schema.tables
--    WHERE table_schema = 'public'
--      AND table_name IN ('users','api_keys','audit_log','comments','settings','tags','webhooks');
--   -- expect 7 rows
--
--   SELECT count(*) FROM information_schema.schemata WHERE schema_name = 'oe_spring_services';
--   -- expect 0
--
--   SELECT count(*) FROM public.flyway_schema_history WHERE version = '36';
--   -- expect 0
