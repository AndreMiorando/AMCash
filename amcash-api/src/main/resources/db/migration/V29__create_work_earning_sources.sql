CREATE TABLE work_earning_sources (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(30) NOT NULL,
    system_key VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_work_earning_sources_system UNIQUE (user_id, system_key)
);

CREATE UNIQUE INDEX uk_work_earning_sources_name
    ON work_earning_sources (user_id, LOWER(name));

CREATE INDEX idx_work_earning_sources_user_order
    ON work_earning_sources (user_id, active, sort_order);
