CREATE TABLE audit_log (
    id          UUID            PRIMARY KEY,
    entity_type VARCHAR(255)    NOT NULL,
    entity_id   UUID            NOT NULL,
    action      VARCHAR(50)     NOT NULL,
    user_name   VARCHAR(255),
    created_at  TIMESTAMP       NOT NULL,
    updated_at  TIMESTAMP       NOT NULL
);
