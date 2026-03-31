ALTER TABLE comments ADD COLUMN task_id UUID REFERENCES tasks(id) ON DELETE CASCADE;

ALTER TABLE comments DROP CONSTRAINT chk_comment_owner;

ALTER TABLE comments ADD CONSTRAINT chk_comment_owner CHECK (
    (CASE WHEN company_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN contact_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN task_id IS NOT NULL THEN 1 ELSE 0 END) = 1
);

CREATE INDEX idx_comments_task_id ON comments(task_id);
