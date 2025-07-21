CREATE TYPE account_status_enum AS ENUM ('ACTIVE', 'SUSPENDED', 'CLOSED');

CREATE TABLE accounts
(
    id             VARCHAR(36) PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id        BIGINT              NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    balance        DECIMAL(15, 2)      NOT NULL DEFAULT 0.00,
    currency       VARCHAR(3)          NOT NULL DEFAULT 'IDR',
    account_status account_status_enum NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP           NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP           NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_account_status ON accounts(account_status);
CREATE INDEX idx_accounts_currency ON accounts(currency);
CREATE INDEX idx_accounts_created_at ON accounts(created_at);
CREATE INDEX idx_accounts_updated_at ON accounts(updated_at);