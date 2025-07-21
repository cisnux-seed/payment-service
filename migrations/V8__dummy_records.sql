-- Insert dummy users
INSERT INTO users (id, username, email, phone, password, created_at, updated_at)
VALUES (1, 'john_doe', 'john.doe@example.com', '+6281234567890',
        '$argon2id$v=19$m=16384,t=2,p=1$PoQbisWKt769EMs6KE+D6Q$dlJIy5ThpYEQMXfeSheNvwy6hfL6cXXdoOJhPmJFAYo',
        '2024-01-15 10:30:00', '2024-01-15 10:30:00'),
       (2, 'jane_smith', 'jane.smith@example.com', '+6281234567891',
        '$argon2id$v=19$m=16384,t=2,p=1$PoQbisWKt769EMs6KE+D6Q$dlJIy5ThpYEQMXfeSheNvwy6hfL6cXXdoOJhPmJFAYo',
        '2024-01-16 14:20:00', '2024-01-16 14:20:00'),
       (3, 'ahmad_budi', 'ahmad.budi@gmail.com', '+6281234567892',
        '$argon2id$v=19$m=16384,t=2,p=1$PoQbisWKt769EMs6KE+D6Q$dlJIy5ThpYEQMXfeSheNvwy6hfL6cXXdoOJhPmJFAYo',
        '2024-01-17 09:15:00', '2024-01-17 09:15:00'),
       (4, 'siti_nurhaliza', 'siti.nurhaliza@yahoo.com', '+6281234567893',
        '$argon2id$v=19$m=16384,t=2,p=1$PoQbisWKt769EMs6KE+D6Q$dlJIy5ThpYEQMXfeSheNvwy6hfL6cXXdoOJhPmJFAYo',
        '2024-01-18 16:45:00', '2024-01-18 16:45:00'),
       (5, 'david_tan', 'david.tan@company.co.id', '+6281234567894',
        '$argon2id$v=19$m=16384,t=2,p=1$PoQbisWKt769EMs6KE+D6Q$dlJIy5ThpYEQMXfeSheNvwy6hfL6cXXdoOJhPmJFAYo',
        '2024-01-19 11:30:00', '2024-01-19 11:30:00'),
       (6, 'rina_kusuma', 'rina.kusuma@outlook.com', '+6281234567895',
        '$argon2id$v=19$m=16384,t=2,p=1$PoQbisWKt769EMs6KE+D6Q$dlJIy5ThpYEQMXfeSheNvwy6hfL6cXXdoOJhPmJFAYo',
        '2024-01-20 08:15:00', '2024-01-20 08:15:00'),
       (7, 'bayu_setiawan', 'bayu.setiawan@gmail.com', '+6285987654321',
        '$argon2id$v=19$m=16384,t=2,p=1$PoQbisWKt769EMs6KE+D6Q$dlJIy5ThpYEQMXfeSheNvwy6hfL6cXXdoOJhPmJFAYo',
        '2024-01-20 10:45:00', '2024-01-20 10:45:00');

-- Insert dummy authentications (refresh tokens)
INSERT INTO authentications (token, user_id, created_at)
VALUES ('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTcwNTM4MDYwMCwiZXhwIjoxNzA1NDY3MDAwfQ.refresh_token_1',
        1, '2024-01-20 10:30:00'),
       ('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqYW5lX3NtaXRoIiwiaWF0IjoxNzA1MzgwNjAwLCJleHAiOjE3MDU0NjcwMDB9.refresh_token_2',
        2, '2024-01-20 14:20:00'),
       ('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhaG1hZF9idWRpIiwiaWF0IjoxNzA1MzgwNjAwLCJleHAiOjE3MDU0NjcwMDB9.refresh_token_3',
        3, '2024-01-20 09:15:00');

-- Insert dummy accounts
INSERT INTO accounts (id, user_id, balance, currency, account_status, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440001', 1, 250000.00, 'IDR', 'ACTIVE', '2024-01-15 10:30:00',
        '2024-01-20 15:45:00'),
       ('550e8400-e29b-41d4-a716-446655440002', 2, 180000.00, 'IDR', 'ACTIVE', '2024-01-16 14:20:00',
        '2024-01-20 16:30:00'),
       ('550e8400-e29b-41d4-a716-446655440003', 3, 75000.00, 'IDR', 'ACTIVE', '2024-01-17 09:15:00',
        '2024-01-20 12:20:00'),
       ('550e8400-e29b-41d4-a716-446655440004', 4, 0.00, 'IDR', 'SUSPENDED', '2024-01-18 16:45:00',
        '2024-01-19 10:00:00'),
       ('550e8400-e29b-41d4-a716-446655440005', 5, 500000.00, 'IDR', 'ACTIVE', '2024-01-19 11:30:00',
        '2024-01-20 17:15:00');

-- Insert dummy historical transactions
INSERT INTO historical_transactions (id, user_id, account_id, transaction_id, transaction_type, transaction_status,
                                     amount, balance_before, balance_after, currency, description, external_reference,
                                     payment_method, metadata, is_accessible_external, created_at, updated_at)
VALUES
-- John Doe transactions
('650e8400-e29b-41d4-a716-446655440001', 1, '550e8400-e29b-41d4-a716-446655440001', 'TXN-20240115-001', 'TOPUP',
 'SUCCESS', 100000.00, 0.00, 100000.00, 'IDR', 'Initial top up via GoPay', 'GOPAY-REF-123456', 'GOPAY',
 '{"provider": "gopay", "reference": "GP123456", "phone": "081234567890"}', true, '2024-01-15 11:00:00',
 '2024-01-15 11:01:00'),
('650e8400-e29b-41d4-a716-446655440002', 1, '550e8400-e29b-41d4-a716-446655440001', 'TXN-20240116-001', 'PAYMENT',
 'SUCCESS', 25000.00, 100000.00, 75000.00, 'IDR', 'Payment for online shopping', 'MERCHANT-REF-789012', 'GOPAY',
 '{"merchant": "Tokopedia", "order_id": "TKP-001"}', true, '2024-01-16 13:30:00', '2024-01-16 13:31:00'),
('650e8400-e29b-41d4-a716-446655440003', 1, '550e8400-e29b-41d4-a716-446655440001', 'TXN-20240118-001', 'TOPUP',
 'SUCCESS', 200000.00, 75000.00, 275000.00, 'IDR', 'Top up via Bank Transfer', 'BNI-TRF-345678', 'SHOPEE_PAY',
 '{"bank": "BNI", "account": "1234567890", "ref": "BNI345678"}', true, '2024-01-18 09:15:00', '2024-01-18 09:20:00'),
('650e8400-e29b-41d4-a716-446655440004', 1, '550e8400-e29b-41d4-a716-446655440001', 'TXN-20240119-001', 'PAYMENT',
 'SUCCESS', 25000.00, 275000.00, 250000.00, 'IDR', 'Food delivery payment', 'GOFOOD-REF-456789', 'GOPAY',
 '{"merchant": "GoFood", "restaurant": "KFC"}', true, '2024-01-19 19:45:00', '2024-01-19 19:46:00'),

-- Jane Smith transactions
('650e8400-e29b-41d4-a716-446655440005', 2, '550e8400-e29b-41d4-a716-446655440002', 'TXN-20240116-002', 'TOPUP',
 'SUCCESS', 150000.00, 0.00, 150000.00, 'IDR', 'First top up via ShopeePay', 'SHOPEE-REF-111222', 'SHOPEE_PAY',
 '{"provider": "shopeepay", "reference": "SP111222"}', true, '2024-01-16 15:00:00', '2024-01-16 15:02:00'),
('650e8400-e29b-41d4-a716-446655440006', 2, '550e8400-e29b-41d4-a716-446655440002', 'TXN-20240117-001', 'PAYMENT',
 'SUCCESS', 45000.00, 150000.00, 105000.00, 'IDR', 'Grab ride payment', 'GRAB-REF-333444', 'SHOPEE_PAY',
 '{"merchant": "Grab", "trip_id": "GRAB123"}', true, '2024-01-17 08:30:00', '2024-01-17 08:31:00'),
('650e8400-e29b-41d4-a716-446655440007', 2, '550e8400-e29b-41d4-a716-446655440002', 'TXN-20240119-002', 'TOPUP',
 'SUCCESS', 75000.00, 105000.00, 180000.00, 'IDR', 'Top up for weekend spending', 'SHOPEE-REF-555666', 'SHOPEE_PAY',
 '{"provider": "shopeepay", "reference": "SP555666"}', true, '2024-01-19 12:00:00', '2024-01-19 12:01:00'),

-- Ahmad Budi transactions
('650e8400-e29b-41d4-a716-446655440008', 3, '550e8400-e29b-41d4-a716-446655440003', 'TXN-20240117-002', 'TOPUP',
 'SUCCESS', 100000.00, 0.00, 100000.00, 'IDR', 'Initial balance setup', 'BCA-TRF-777888', 'SHOPEE_PAY',
 '{"bank": "BCA", "account": "9876543210", "ref": "BCA777888"}', true, '2024-01-17 10:00:00', '2024-01-17 10:05:00'),
('650e8400-e29b-41d4-a716-446655440009', 3, '550e8400-e29b-41d4-a716-446655440003', 'TXN-20240118-002', 'PAYMENT',
 'SUCCESS', 25000.00, 100000.00, 75000.00, 'IDR', 'Movie ticket purchase', 'CGV-REF-999000', 'GOPAY',
 '{"merchant": "CGV", "movie": "Avengers", "tickets": 2}', true, '2024-01-18 20:15:00', '2024-01-18 20:16:00'),

-- Siti Nurhaliza transactions (suspended account)
('650e8400-e29b-41d4-a716-446655440010', 4, '550e8400-e29b-41d4-a716-446655440004', 'TXN-20240118-003', 'TOPUP',
 'FAILED', 50000.00, 0.00, 0.00, 'IDR', 'Failed top up - insufficient funds', 'MANDIRI-REF-FAIL', 'SHOPEE_PAY',
 '{"bank": "Mandiri", "error": "insufficient_funds"}', false, '2024-01-18 17:00:00', '2024-01-18 17:05:00'),

-- David Tan transactions
('650e8400-e29b-41d4-a716-446655440011', 5, '550e8400-e29b-41d4-a716-446655440005', 'TXN-20240119-003', 'TOPUP',
 'SUCCESS', 500000.00, 0.00, 500000.00, 'IDR', 'Large business top up', 'BNI-CORP-123456', 'SHOPEE_PAY',
 '{"bank": "BNI", "account": "1111222233", "type": "corporate", "ref": "BNICORP123456"}', true, '2024-01-19 11:45:00',
 '2024-01-19 11:50:00'),
('650e8400-e29b-41d4-a716-446655440012', 5, '550e8400-e29b-41d4-a716-446655440005', 'TXN-20240120-001', 'TRANSFER',
 'PENDING', 50000.00, 500000.00, 450000.00, 'IDR', 'Transfer to employee bonus', 'TRANSFER-REF-001', null,
 '{"transfer_type": "internal", "recipient": "employee_bonus_pool"}', true, '2024-01-20 14:30:00',
 '2024-01-20 14:30:00');

-- Insert dummy external services
INSERT INTO external_services (id, company, service_type, contact_email, description, is_active, created_at, updated_at)
VALUES ('750e8400-e29b-41d4-a716-446655440001', 'FinanceHub Analytics', 'HISTORICAL_TRANSACTION',
        'api@financehub.co.id', 'Financial analytics and reporting service for transaction insights', true,
        '2024-01-10 09:00:00', '2024-01-10 09:00:00'),
       ('750e8400-e29b-41d4-a716-446655440002', 'DataMining Corp', 'HISTORICAL_TRANSACTION',
        'integration@datamining.com', 'Data mining service for transaction pattern analysis', true,
        '2024-01-12 14:30:00', '2024-01-12 14:30:00'),
       ('750e8400-e29b-41d4-a716-446655440003', 'TaxReporting Solutions', 'HISTORICAL_TRANSACTION',
        'support@taxreporting.co.id', 'Automated tax reporting and compliance service', true, '2024-01-14 11:15:00',
        '2024-01-14 11:15:00'),
       ('750e8400-e29b-41d4-a716-446655440004', 'AuditTrail Systems', 'HISTORICAL_TRANSACTION', 'audit@audittrail.net',
        'Audit trail and compliance monitoring system', false, '2024-01-08 16:20:00', '2024-01-15 10:00:00');

-- Insert dummy API keys
INSERT INTO api_keys (id, external_service_id, key_name, api_key, permissions, rate_limit_count, rate_limit_unit,
                      expires_at, is_active, created_at, updated_at)
VALUES ('850e8400-e29b-41d4-a716-446655440001', '750e8400-e29b-41d4-a716-446655440001', 'Production API Key',
        '$2a$10$financehub.prod.key.hash.example', 'READ_TRANSACTIONS,READ_REPORTS', 1000, 'hour',
        '2024-12-31 23:59:59', true, '2024-01-10 09:15:00', '2024-01-10 09:15:00'),
       ('850e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440001', 'Development API Key',
        '$2a$10$financehub.dev.key.hash.example', 'READ_TRANSACTIONS', 100, 'hour', '2024-06-30 23:59:59', true,
        '2024-01-10 09:30:00', '2024-01-10 09:30:00'),
       ('850e8400-e29b-41d4-a716-446655440003', '750e8400-e29b-41d4-a716-446655440002', 'DataMining Main Key',
        '$2a$10$datamining.main.key.hash.example', 'READ_TRANSACTIONS', 2000, 'hour', '2025-01-31 23:59:59', true,
        '2024-01-12 14:45:00', '2024-01-12 14:45:00'),
       ('850e8400-e29b-41d4-a716-446655440004', '750e8400-e29b-41d4-a716-446655440003', 'Tax Service Key',
        '$2a$10$taxreporting.service.key.hash.example', 'READ_TRANSACTIONS,READ_REPORTS', 500, 'hour',
        '2024-12-31 23:59:59', true, '2024-01-14 11:30:00', '2024-01-14 11:30:00'),
       ('850e8400-e29b-41d4-a716-446655440005', '750e8400-e29b-41d4-a716-446655440004', 'Audit System Key',
        '$2a$10$audit.system.key.hash.example', 'READ_TRANSACTIONS', 200, 'hour', '2024-03-31 23:59:59', false,
        '2024-01-08 16:30:00', '2024-01-15 10:00:00'),
       ('850e8400-e29b-41d4-a716-446655440006', '750e8400-e29b-41d4-a716-446655440001', 'Burst Testing Key',
        '$2a$10$financehub.burst.key.hash.example', 'READ_TRANSACTIONS', 10, 'minute', '2024-03-31 23:59:59', true,
        '2024-01-15 10:00:00', '2024-01-15 10:00:00'),
       ('850e8400-e29b-41d4-a716-446655440007', '750e8400-e29b-41d4-a716-446655440002', 'Daily Batch Key',
        '$2a$10$datamining.batch.key.hash.example', 'READ_TRANSACTIONS,READ_REPORTS', 50000, 'day',
        '2024-12-31 23:59:59', true, '2024-01-16 08:00:00', '2024-01-16 08:00:00'),
       ('850e8400-e29b-41d4-a716-446655440008', '750e8400-e29b-41d4-a716-446655440003', 'Real-time Key',
        '$2a$10$taxreporting.realtime.key.hash.example', 'READ_TRANSACTIONS', 5, 'second', '2024-06-30 23:59:59', true,
        '2024-01-17 12:00:00', '2024-01-17 12:00:00');

-- Insert dummy API access logs
INSERT INTO api_access_logs (id, external_service_id, api_key_id, endpoint, http_method, ip_address, user_agent,
                             query_params, response_status, response_time_ms, error_message, created_at)
VALUES ('950e8400-e29b-41d4-a716-446655440001', '750e8400-e29b-41d4-a716-446655440001',
        '850e8400-e29b-41d4-a716-446655440001', '/api/external/transactions', 'GET', '203.34.118.1',
        'FinanceHub-Client/1.2.0', 'page=0&size=20&type=TOPUP', 200, 245, null, '2024-01-20 10:15:00'),
       ('950e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440001',
        '850e8400-e29b-41d4-a716-446655440001', '/api/external/transactions', 'GET', '203.34.118.1',
        'FinanceHub-Client/1.2.0', 'page=1&size=20&type=PAYMENT', 200, 189, null, '2024-01-20 10:16:00'),
       ('950e8400-e29b-41d4-a716-446655440003', '750e8400-e29b-41d4-a716-446655440002',
        '850e8400-e29b-41d4-a716-446655440003', '/api/external/transactions', 'GET', '45.112.89.234',
        'DataMiner-Bot/2.1', 'page=0&size=100&fromDate=2024-01-01T00:00:00Z', 200, 567, null, '2024-01-20 11:30:00'),
       ('950e8400-e29b-41d4-a716-446655440004', '750e8400-e29b-41d4-a716-446655440001',
        '850e8400-e29b-41d4-a716-446655440002', '/api/external/transactions', 'GET', '203.34.118.2',
        'FinanceHub-Dev/0.9.0', 'page=0&size=5', 401, 45, 'Invalid API key', '2024-01-20 12:00:00'),
       ('950e8400-e29b-41d4-a716-446655440005', '750e8400-e29b-41d4-a716-446655440003',
        '850e8400-e29b-41d4-a716-446655440004', '/api/transactions/TXN-20240115-001', 'GET', '112.215.200.45',
        'TaxService/3.0.1', null, 200, 123, null, '2024-01-20 13:45:00'),
       ('950e8400-e29b-41d4-a716-446655440006', '750e8400-e29b-41d4-a716-446655440002',
        '850e8400-e29b-41d4-a716-446655440003', '/api/external/transactions', 'GET', '45.112.89.234',
        'DataMiner-Bot/2.1', 'page=0&size=1000', 400, 23, 'Page size exceeds maximum limit of 100',
        '2024-01-20 14:00:00'),
       ('950e8400-e29b-41d4-a716-446655440007', '750e8400-e29b-41d4-a716-446655440001',
        '850e8400-e29b-41d4-a716-446655440001', '/api/external/transactions', 'GET', '203.34.118.1',
        'FinanceHub-Client/1.2.0',
        'page=0&size=50&status=SUCCESS&fromDate=2024-01-15T00:00:00Z&toDate=2024-01-20T23:59:59Z', 200, 334, null,
        '2024-01-20 15:30:00'),
       ('950e8400-e29b-41d4-a716-446655440008', '750e8400-e29b-41d4-a716-446655440004',
        '850e8400-e29b-41d4-a716-446655440005', '/api/external/transactions', 'GET', '192.168.1.100', 'AuditBot/1.0',
        'page=0&size=20', 401, 12, 'API key is inactive', '2024-01-20 16:00:00');
