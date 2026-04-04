CREATE TABLE webhooks (
    id         UUID         PRIMARY KEY,
    url        VARCHAR(2048) NOT NULL,
    active     BOOLEAN       NOT NULL DEFAULT true,
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL
);
