CREATE TABLE api_keys (
    id         UUID          PRIMARY KEY,
    name       VARCHAR(255)  NOT NULL,
    key_hash   VARCHAR(64)   NOT NULL UNIQUE,
    key_prefix VARCHAR(20)   NOT NULL,
    created_by VARCHAR(255)  NOT NULL,
    created_at TIMESTAMP     NOT NULL
);
