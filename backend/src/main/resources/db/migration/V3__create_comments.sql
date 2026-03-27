CREATE TABLE comments (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    text        TEXT            NOT NULL,
    author      VARCHAR(255)    NOT NULL,
    company_id  UUID            REFERENCES companies(id),
    contact_id  UUID            REFERENCES contacts(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_comment_owner CHECK (
        (company_id IS NOT NULL AND contact_id IS NULL) OR
        (company_id IS NULL AND contact_id IS NOT NULL)
    )
);

CREATE INDEX idx_comments_company_id ON comments(company_id);
CREATE INDEX idx_comments_contact_id ON comments(contact_id);
