-- Refactor settings table to match AbstractEntity structure from spring-services
-- Add UUID id column, make key a regular unique column instead of PK

ALTER TABLE settings DROP CONSTRAINT settings_pkey;

ALTER TABLE settings ADD COLUMN id UUID;
UPDATE settings SET id = gen_random_uuid();
ALTER TABLE settings ALTER COLUMN id SET NOT NULL;
ALTER TABLE settings ADD PRIMARY KEY (id);

ALTER TABLE settings ADD CONSTRAINT uq_settings_key UNIQUE (key);

-- Convert timestamptz to timestamp to match Hibernate Instant mapping
ALTER TABLE settings ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC';
ALTER TABLE settings ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'UTC';
ALTER TABLE settings ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE settings ALTER COLUMN updated_at DROP DEFAULT;