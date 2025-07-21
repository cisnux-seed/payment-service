CREATE TYPE http_method_enum AS ENUM ('GET', 'POST', 'PUT', 'DELETE', 'PATCH');

CREATE TABLE api_access_logs
(
    id                  VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    external_service_id VARCHAR(36) NOT NULL REFERENCES external_services (id) ON DELETE CASCADE,
    api_key_id          VARCHAR(36) NOT NULL REFERENCES api_keys (id) ON DELETE CASCADE,
    endpoint            VARCHAR(255),
    http_method         http_method_enum,
    ip_address          TEXT,
    user_agent          TEXT,
    query_params        TEXT,
    response_status     INTEGER,
    response_time_ms    INTEGER,
    error_message       TEXT,
    created_at          TIMESTAMP   NOT NULL    DEFAULT now()
);

CREATE INDEX idx_api_access_logs_external_service_id ON api_access_logs(external_service_id);
CREATE INDEX idx_api_access_logs_api_key_id ON api_access_logs(api_key_id);
CREATE INDEX idx_api_access_logs_created_at ON api_access_logs(created_at DESC);
CREATE INDEX idx_api_access_logs_endpoint ON api_access_logs(endpoint);
CREATE INDEX idx_api_access_logs_http_method ON api_access_logs(http_method);
CREATE INDEX idx_api_access_logs_response_status ON api_access_logs(response_status);
CREATE INDEX idx_api_access_logs_ip_address ON api_access_logs(ip_address);
CREATE INDEX idx_api_access_logs_service_created ON api_access_logs(external_service_id, created_at DESC);
CREATE INDEX idx_api_access_logs_key_created ON api_access_logs(api_key_id, created_at DESC);
CREATE INDEX idx_api_access_logs_endpoint_method_status ON api_access_logs(endpoint, http_method, response_status);
CREATE INDEX idx_api_access_logs_response_time ON api_access_logs(response_time_ms);