-- Aligns the `users` table with spring-services 1.1.0, which expanded the
-- UserEntity / UserInformation model:
--   * `sub`         becomes nullable (non-JWT users no longer require it)
--   * `external_id` new, nullable, unique  (stable IdP-side identifier)
--   * `user_name`   new, NOT NULL, unique   (preferred_username claim)
--   * `active`      new, NOT NULL, default true (deactivated accounts are rejected)
--
-- `user_name` is added nullable first, backfilled from `sub` for existing rows
-- (so the unique NOT NULL constraint can be enforced afterwards), per the
-- spring-services upgrade guide.

ALTER TABLE users ALTER COLUMN sub DROP NOT NULL;

ALTER TABLE users ADD COLUMN external_id VARCHAR(255);
ALTER TABLE users ADD COLUMN user_name   VARCHAR(255);
ALTER TABLE users ADD COLUMN active      BOOLEAN NOT NULL DEFAULT true;

UPDATE users SET user_name = sub WHERE user_name IS NULL;

ALTER TABLE users ALTER COLUMN user_name SET NOT NULL;

ALTER TABLE users ADD CONSTRAINT uq_users_user_name   UNIQUE (user_name);
ALTER TABLE users ADD CONSTRAINT uq_users_external_id UNIQUE (external_id);
