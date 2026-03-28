-- Drop the existing unique index
DROP INDEX IF EXISTS idx_companies_brevo_company_id;

-- Change column type from BIGINT to VARCHAR(50)
ALTER TABLE companies ALTER COLUMN brevo_company_id TYPE VARCHAR(50);

-- Recreate unique index
CREATE UNIQUE INDEX idx_companies_brevo_company_id
    ON companies(brevo_company_id) WHERE brevo_company_id IS NOT NULL;

-- Clean up bad data: reset the single company with brevoCompanyId='0'
UPDATE companies SET brevo_company_id = NULL WHERE brevo_company_id = '0';
