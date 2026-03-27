CREATE TABLE companies (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255)    NOT NULL,
    email       VARCHAR(255),
    website     VARCHAR(500),
    street      VARCHAR(255),
    house_number VARCHAR(20),
    zip_code    VARCHAR(20),
    city        VARCHAR(255),
    country     VARCHAR(100),
    deleted     BOOLEAN         NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);
