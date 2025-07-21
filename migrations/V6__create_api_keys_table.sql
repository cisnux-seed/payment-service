CREATE TYPE permissions_enum AS ENUM ('READ_TRANSACTIONS', 'READ_REPORTS',
    'READ_TRANSACTIONS,READ_REPORTS');
CREATE TYPE rate_limit_unit_enum AS ENUM ('second', 'minute', 'hour', 'day');

CREATE TABLE api_keys
(
    id                  VARCHAR(36) PRIMARY KEY   DEFAULT gen_random_uuid(),
    external_service_id VARCHAR(36)      NOT NULL REFERENCES external_services (id) ON DELETE CASCADE,
    key_name            VARCHAR(255)     NOT NULL,
    api_key             VARCHAR(255)     NOT NULL UNIQUE,
    permissions         permissions_enum NOT NULL,
    rate_limit_count    INTEGER,
    rate_limit_unit     rate_limit_unit_enum NOT NULL DEFAULT 'minute',
    expires_at          TIMESTAMP,
    is_active           BOOLEAN          NOT NULL DEFAULT true,
    created_at          TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP        NOT NULL DEFAULT now()
);

CREATE INDEX idx_api_keys_external_service_id ON api_keys(external_service_id);
CREATE INDEX idx_api_keys_api_key ON api_keys(api_key);
CREATE INDEX idx_api_keys_is_active ON api_keys(is_active);
CREATE INDEX idx_api_keys_expires_at ON api_keys(expires_at);
CREATE INDEX idx_api_keys_permissions ON api_keys(permissions);
CREATE INDEX idx_api_keys_created_at ON api_keys(created_at);
CREATE INDEX idx_api_keys_rate_limit_count ON api_keys(rate_limit_count);
CREATE INDEX idx_api_keys_rate_limit_unit ON api_keys(rate_limit_unit);
CREATE INDEX idx_api_keys_rate_limit_composite ON api_keys(rate_limit_count, rate_limit_unit, is_active);