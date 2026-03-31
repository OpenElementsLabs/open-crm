CREATE TABLE users (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    sub                 VARCHAR(255)    NOT NULL UNIQUE,
    name                VARCHAR(255)    NOT NULL,
    email               VARCHAR(255),
    avatar              BYTEA,
    avatar_content_type VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_sub ON users(sub);
