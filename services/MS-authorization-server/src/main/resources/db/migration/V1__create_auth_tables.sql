-- V1: Authorization Server tables
-- Clients, authorizations, tokens

-- Registered Clients (from DCR)
CREATE TABLE registered_clients (
    id                          VARCHAR(100) PRIMARY KEY,
    client_id                   VARCHAR(100) NOT NULL UNIQUE,
    client_id_issued_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    client_name                 VARCHAR(200),
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types   VARCHAR(1000) NOT NULL,
    redirect_uris               VARCHAR(2000),
    scopes                      VARCHAR(1000) NOT NULL,
    token_settings              TEXT,
    client_settings             TEXT,
    software_id                 VARCHAR(100),
    software_statement          TEXT,
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clients_client_id ON registered_clients(client_id);
CREATE INDEX idx_clients_software_id ON registered_clients(software_id);

-- OAuth2 Authorizations (active grants)
CREATE TABLE oauth2_authorizations (
    id                          VARCHAR(100) PRIMARY KEY,
    registered_client_id        VARCHAR(100) NOT NULL,
    principal_name              VARCHAR(200) NOT NULL,
    authorization_grant_type    VARCHAR(100) NOT NULL,
    authorized_scopes           VARCHAR(1000),
    attributes                  TEXT,
    state                       VARCHAR(500),
    authorization_code_value    TEXT,
    authorization_code_issued_at TIMESTAMP WITH TIME ZONE,
    authorization_code_expires_at TIMESTAMP WITH TIME ZONE,
    access_token_value          TEXT,
    access_token_issued_at      TIMESTAMP WITH TIME ZONE,
    access_token_expires_at     TIMESTAMP WITH TIME ZONE,
    access_token_scopes         VARCHAR(1000),
    refresh_token_value         TEXT,
    refresh_token_issued_at     TIMESTAMP WITH TIME ZONE,
    refresh_token_expires_at    TIMESTAMP WITH TIME ZONE,
    consent_id                  VARCHAR(100),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auth_client ON oauth2_authorizations(registered_client_id);
CREATE INDEX idx_auth_principal ON oauth2_authorizations(principal_name);
CREATE INDEX idx_auth_consent ON oauth2_authorizations(consent_id);

-- OAuth2 Authorization Consents
CREATE TABLE oauth2_authorization_consents (
    registered_client_id        VARCHAR(100) NOT NULL,
    principal_name              VARCHAR(200) NOT NULL,
    authorities                 VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- JTI tracking (replay prevention for private_key_jwt)
-- Note: In production this is Redis-based for performance
CREATE TABLE jti_tracking (
    jti                         VARCHAR(200) PRIMARY KEY,
    client_id                   VARCHAR(100) NOT NULL,
    expires_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jti_expires ON jti_tracking(expires_at);

COMMENT ON TABLE registered_clients IS 'TPPs registrados via DCR';
COMMENT ON TABLE oauth2_authorizations IS 'Grants activos (codes, tokens)';
COMMENT ON TABLE jti_tracking IS 'Prevención de replay en private_key_jwt';
