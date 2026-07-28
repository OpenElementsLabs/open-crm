CREATE TABLE opportunities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           TEXT NOT NULL,
    stage           TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    product         TEXT,
    estimated_value NUMERIC(12,2) CHECK (estimated_value >= 0),
    company_id      UUID NOT NULL REFERENCES companies(id),
    main_contact_id UUID NOT NULL REFERENCES contacts(id),
    owner_id        UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_opportunities_company_id ON opportunities(company_id);
CREATE INDEX idx_opportunities_main_contact_id ON opportunities(main_contact_id);
CREATE INDEX idx_opportunities_owner_id ON opportunities(owner_id);
CREATE INDEX idx_opportunities_status ON opportunities(status);

CREATE TABLE opportunity_contacts (
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    contact_id     UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    PRIMARY KEY (opportunity_id, contact_id)
);

CREATE TABLE opportunity_tags (
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    tag_id         UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (opportunity_id, tag_id)
);

CREATE TABLE opportunity_comments (
    comment_id     UUID PRIMARY KEY REFERENCES comments(id) ON DELETE CASCADE,
    opportunity_id UUID NOT NULL REFERENCES opportunities(id)
);
