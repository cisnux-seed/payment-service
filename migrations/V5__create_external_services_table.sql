CREATE TABLE external_services
(
    id            VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    company       VARCHAR(255) NOT NULL UNIQUE,
    service_type  VARCHAR(50)  NOT NULL   DEFAULT 'HISTORICAL_TRANSACTION',
    contact_email VARCHAR(255) NOT NULL,
    description   TEXT,
    is_active     BOOLEAN      NOT NULL   DEFAULT true,
    created_at    TIMESTAMP    NOT NULL   DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL   DEFAULT now()
);

CREATE INDEX idx_external_services_company ON external_services(company);
CREATE INDEX idx_external_services_service_type ON external_services(service_type);
CREATE INDEX idx_external_services_is_active ON external_services(is_active);
CREATE INDEX idx_external_services_created_at ON external_services(created_at);