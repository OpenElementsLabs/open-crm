ALTER TABLE companies DROP COLUMN deleted;

ALTER TABLE contacts DROP CONSTRAINT IF EXISTS contacts_company_id_fkey;
ALTER TABLE contacts ADD CONSTRAINT contacts_company_id_fkey FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL;

ALTER TABLE comments DROP CONSTRAINT IF EXISTS comments_company_id_fkey;
ALTER TABLE comments ADD CONSTRAINT comments_company_id_fkey FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_company_id_fkey;
ALTER TABLE tasks ADD CONSTRAINT tasks_company_id_fkey FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;
