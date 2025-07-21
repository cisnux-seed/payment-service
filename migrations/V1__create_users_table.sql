CREATE TABLE users(
    id         SERIAL PRIMARY KEY,
    username   VARCHAR(255)  NOT NULL UNIQUE,
    email      VARCHAR(255)  NOT NULL UNIQUE,
    phone      VARCHAR(20)   NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_phone ON users(phone);