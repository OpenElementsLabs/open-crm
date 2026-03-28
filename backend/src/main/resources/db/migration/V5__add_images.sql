ALTER TABLE companies ADD COLUMN logo BYTEA;
ALTER TABLE companies ADD COLUMN logo_content_type VARCHAR(50);

ALTER TABLE contacts ADD COLUMN photo BYTEA;
ALTER TABLE contacts ADD COLUMN photo_content_type VARCHAR(50);
