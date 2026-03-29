DROP INDEX IF EXISTS idx_contacts_brevo_id;
ALTER TABLE contacts ALTER COLUMN brevo_id TYPE VARCHAR(50);
CREATE UNIQUE INDEX idx_contacts_brevo_id ON contacts(brevo_id) WHERE brevo_id IS NOT NULL;
ALTER TABLE contacts DROP COLUMN synced_to_brevo;
