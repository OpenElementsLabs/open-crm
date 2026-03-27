CREATE TABLE contacts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(255)    NOT NULL,
    last_name       VARCHAR(255)    NOT NULL,
    email           VARCHAR(255),
    position        VARCHAR(255),
    gender          VARCHAR(20),
    linkedin_url    VARCHAR(500),
    phone_number    VARCHAR(50),
    company_id      UUID            REFERENCES companies(id),
    synced_to_brevo BOOLEAN         NOT NULL DEFAULT false,
    double_opt_in   BOOLEAN         NOT NULL DEFAULT false,
    language        VARCHAR(5)      NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_contacts_company_id ON contacts(company_id);
