CREATE TABLE authentications
(
    token      VARCHAR(512) PRIMARY KEY,
    user_id    SERIAL NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_authentications_user_id ON authentications(user_id);
CREATE INDEX idx_authentications_created_at ON authentications(created_at);