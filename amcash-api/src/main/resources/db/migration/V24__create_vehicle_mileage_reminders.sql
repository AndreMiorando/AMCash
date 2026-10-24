CREATE TABLE vehicle_mileage_reminders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL UNIQUE REFERENCES vehicles(id) ON DELETE CASCADE,
    reminder_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_vehicle_mileage_reminder_user
    ON vehicle_mileage_reminders(user_id, active);
