ALTER TABLE operational_mileage_notifications
    DROP CONSTRAINT operational_mileage_notifications_configuration_id_key;

ALTER TABLE operational_mileage_notifications
    ADD COLUMN period_start DATE,
    ADD COLUMN period_end DATE,
    ADD COLUMN evaluated_at DATE,
    ADD COLUMN evidence_key VARCHAR(64),
    ADD COLUMN consecutive_relevant INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN rejection_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_notification_at DATE;

UPDATE operational_mileage_notifications
SET period_start = CURRENT_DATE,
    period_end = CURRENT_DATE,
    evaluated_at = CURRENT_DATE,
    evidence_key = 'legacy-' || id,
    consecutive_relevant = 0,
    active = FALSE,
    read_at = NULL;

ALTER TABLE operational_mileage_notifications
    ALTER COLUMN period_start SET NOT NULL,
    ALTER COLUMN period_end SET NOT NULL,
    ALTER COLUMN evaluated_at SET NOT NULL,
    ALTER COLUMN evidence_key SET NOT NULL,
    ADD CONSTRAINT uk_operational_mileage_configuration_source UNIQUE(configuration_id, suggestion_source);

ALTER TABLE vehicles ADD COLUMN in_use BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE vehicles SET in_use = TRUE WHERE id IN (
    SELECT DISTINCT ON (user_id) id FROM vehicles ORDER BY user_id, created_at
);
CREATE UNIQUE INDEX uk_vehicles_user_in_use ON vehicles(user_id) WHERE in_use;

ALTER TABLE work_days ADD COLUMN vehicle_id UUID REFERENCES vehicles(id) ON DELETE SET NULL;
CREATE INDEX idx_work_days_vehicle_date ON work_days(vehicle_id, work_date);
