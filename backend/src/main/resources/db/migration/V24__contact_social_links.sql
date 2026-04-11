-- Create contact_social_links table
CREATE TABLE contact_social_links (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    network_type VARCHAR(20) NOT NULL,
    value VARCHAR(500) NOT NULL,
    url VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_contact_social_links_contact_id ON contact_social_links(contact_id);

-- Migrate existing LinkedIn URLs
INSERT INTO contact_social_links (contact_id, network_type, value, url)
SELECT id, 'LINKEDIN', linkedin_url, linkedin_url
FROM contacts
WHERE linkedin_url IS NOT NULL AND linkedin_url <> '';

-- Drop the old column
ALTER TABLE contacts DROP COLUMN linkedin_url;
