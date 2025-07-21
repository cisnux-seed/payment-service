CREATE TYPE transaction_type_enum AS ENUM ('TOPUP', 'PAYMENT', 'REFUND', 'TRANSFER');
CREATE TYPE transaction_status_enum AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED');
CREATE TYPE payment_method_enum AS ENUM ('GOPAY', 'SHOPEE_PAY');

CREATE TABLE historical_transactions
(
    id                     VARCHAR(36) PRIMARY KEY          DEFAULT gen_random_uuid(),
    user_id                BIGINT                  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    account_id             VARCHAR(36)             NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    transaction_id         VARCHAR(50)             NOT NULL UNIQUE,
    transaction_type       transaction_type_enum   NOT NULL,
    transaction_status     transaction_status_enum NOT NULL,
    amount                 DECIMAL(15, 2)          NOT NULL,
    balance_before         DECIMAL(15, 2)          NOT NULL,
    balance_after          DECIMAL(15, 2)          NOT NULL,
    currency               VARCHAR(3)              NOT NULL DEFAULT 'IDR',
    description            TEXT,
    external_reference     VARCHAR(255),
    payment_method         payment_method_enum,
    metadata               TEXT,
    is_accessible_external BOOLEAN                 NOT NULL DEFAULT true,
    created_at             TIMESTAMP               NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP               NOT NULL DEFAULT now()
);

CREATE INDEX idx_historical_transactions_user_id ON historical_transactions(user_id);
CREATE INDEX idx_historical_transactions_account_id ON historical_transactions(account_id);
CREATE INDEX idx_historical_transactions_transaction_id ON historical_transactions(transaction_id);
CREATE INDEX idx_historical_transactions_transaction_type ON historical_transactions(transaction_type);
CREATE INDEX idx_historical_transactions_transaction_status ON historical_transactions(transaction_status);
CREATE INDEX idx_historical_transactions_payment_method ON historical_transactions(payment_method);
CREATE INDEX idx_historical_transactions_created_at ON historical_transactions(created_at);
CREATE INDEX idx_historical_transactions_updated_at ON historical_transactions(updated_at);
CREATE INDEX idx_historical_transactions_external_reference ON historical_transactions(external_reference);
CREATE INDEX idx_historical_transactions_is_accessible_external ON historical_transactions(is_accessible_external);

CREATE INDEX idx_historical_transactions_user_type_status ON historical_transactions(user_id, transaction_type, transaction_status, payment_method);
CREATE INDEX idx_historical_transactions_user_created_at ON historical_transactions(user_id, created_at DESC);
CREATE INDEX idx_historical_transactions_type_status_created ON historical_transactions(transaction_type, transaction_status, created_at DESC);
CREATE INDEX idx_historical_transactions_status_created ON historical_transactions(transaction_status, created_at DESC);
CREATE INDEX idx_historical_transactions_accessible_created ON historical_transactions(is_accessible_external, created_at DESC);