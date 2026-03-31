CREATE TABLE tasks (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    action      TEXT            NOT NULL,
    due_date    DATE            NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    company_id  UUID            REFERENCES companies(id),
    contact_id  UUID            REFERENCES contacts(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_task_owner CHECK (
        (company_id IS NOT NULL AND contact_id IS NULL) OR
        (company_id IS NULL AND contact_id IS NOT NULL)
    )
);

CREATE INDEX idx_tasks_company_id ON tasks(company_id);
CREATE INDEX idx_tasks_contact_id ON tasks(contact_id);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_status ON tasks(status);

CREATE TABLE task_tags (
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tag_id  UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, tag_id)
);
