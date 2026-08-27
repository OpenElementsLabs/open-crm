-- Move the seven spring-services library tables into their own schema.
--
-- As of spring-services 1.3.0 every library entity declares
-- @Table(schema = "oe_spring_services"). The library ships no runtime migrations, so CRM's
-- own Flyway timeline (V3, V7, V12, V16, V20, V22, V28) created these tables in `public` and
-- CRM must move them to match.
--
-- ALTER TABLE ... SET SCHEMA moves each table together with its constraints and indexes.
-- PostgreSQL resolves foreign keys by object identity, not by qualified name, so the eleven
-- cross-schema foreign keys from CRM-owned tables (opportunities.owner_id, *_tags.tag_id,
-- *_comments.comment_id, audit_log.user_id, comments.author_id) follow automatically and stay
-- valid. PostgreSQL runs DDL transactionally and Flyway wraps this migration in a transaction,
-- so the whole move is atomic.
--
-- Same path on every environment: on a fresh database V1-V35 create the tables in `public`
-- first, then V36 moves them, exactly as in production. No fresh/existing branching is needed.

CREATE SCHEMA IF NOT EXISTS oe_spring_services;

ALTER TABLE users     SET SCHEMA oe_spring_services;
ALTER TABLE api_keys  SET SCHEMA oe_spring_services;
ALTER TABLE audit_log SET SCHEMA oe_spring_services;
ALTER TABLE comments  SET SCHEMA oe_spring_services;
ALTER TABLE settings  SET SCHEMA oe_spring_services;
ALTER TABLE tags      SET SCHEMA oe_spring_services;
ALTER TABLE webhooks  SET SCHEMA oe_spring_services;
