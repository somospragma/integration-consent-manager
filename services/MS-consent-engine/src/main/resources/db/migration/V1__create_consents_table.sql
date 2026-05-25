-- V1: Create consents table and consent_events table
-- Consent Manager - Open Finance

CREATE TABLE consents (
    consent_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(30) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    tpp_id          VARCHAR(100) NOT NULL,
    user_id         VARCHAR(100),
    permissions     JSONB NOT NULL DEFAULT '[]',
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    authorized_at   TIMESTAMP WITH TIME ZONE,
    revoked_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    metadata        JSONB,
    country_code    VARCHAR(3) NOT NULL DEFAULT 'COL',
    redirect_url    VARCHAR(500)
);

-- Indexes
CREATE INDEX idx_consents_user_id ON consents(user_id);
CREATE INDEX idx_consents_tpp_id ON consents(tpp_id);
CREATE INDEX idx_consents_status ON consents(status);
CREATE INDEX idx_consents_type ON consents(type);
CREATE INDEX idx_consents_expires_at ON consents(expires_at);
CREATE INDEX idx_consents_created_at ON consents(created_at);
CREATE INDEX idx_consents_tpp_status ON consents(tpp_id, status);

-- Consent Events (state change history)
CREATE TABLE consent_events (
    event_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consent_id      UUID NOT NULL REFERENCES consents(consent_id),
    event_type      VARCHAR(50) NOT NULL,
    previous_status VARCHAR(30),
    new_status      VARCHAR(30) NOT NULL,
    actor_id        VARCHAR(100) NOT NULL,
    actor_type      VARCHAR(20) NOT NULL,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consent_events_consent_id ON consent_events(consent_id);
CREATE INDEX idx_consent_events_created_at ON consent_events(created_at);

-- Comments
COMMENT ON TABLE consents IS 'Tabla principal de consentimientos Open Finance';
COMMENT ON TABLE consent_events IS 'Historial de cambios de estado de consentimientos';
COMMENT ON COLUMN consents.permissions IS 'Array JSON de permisos otorgados (READ_ACCOUNTS_BASIC, etc.)';
COMMENT ON COLUMN consents.metadata IS 'Metadata adicional (accountIds, tppName, authMethod)';
COMMENT ON COLUMN consents.country_code IS 'País para particionamiento multi-región';
