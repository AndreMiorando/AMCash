CREATE TABLE users (
    id UUID PRIMARY KEY,
    google_subject VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    picture_url VARCHAR(2048),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_google_subject ON users (google_subject);
