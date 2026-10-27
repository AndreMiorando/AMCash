ALTER TABLE users ADD COLUMN deletion_requested_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_users_deletion_requested_at
    ON users (deletion_requested_at)
    WHERE deletion_requested_at IS NOT NULL;
