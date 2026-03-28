-- Brevo company ID for matching after first import
ALTER TABLE companies ADD COLUMN brevo_company_id BIGINT;
CREATE UNIQUE INDEX idx_companies_brevo_company_id
    ON companies(brevo_company_id) WHERE brevo_company_id IS NOT NULL;

-- Brevo contact ID for matching after first import
ALTER TABLE contacts ADD COLUMN brevo_id BIGINT;
CREATE UNIQUE INDEX idx_contacts_brevo_id
    ON contacts(brevo_id) WHERE brevo_id IS NOT NULL;

-- Generic key-value settings table
CREATE TABLE settings (
    key         VARCHAR(100)    PRIMARY KEY,
    value       TEXT            NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);
