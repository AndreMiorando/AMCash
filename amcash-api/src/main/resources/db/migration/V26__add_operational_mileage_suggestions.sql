ALTER TABLE operational_cost_configurations
    ADD COLUMN mileage_source VARCHAR(16) NOT NULL DEFAULT 'MANUAL';

CREATE TABLE operational_mileage_notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    configuration_id UUID NOT NULL UNIQUE REFERENCES operational_cost_configurations(id) ON DELETE CASCADE,
    suggestion_source VARCHAR(16) NOT NULL,
    suggested_mileage NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_operational_mileage_notification_user
    ON operational_mileage_notifications(user_id, active);
