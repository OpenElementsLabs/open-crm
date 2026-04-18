ALTER TABLE contact_social_links ADD COLUMN updated_at TIMESTAMPTZ;
UPDATE contact_social_links SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE contact_social_links ALTER COLUMN updated_at SET NOT NULL;
