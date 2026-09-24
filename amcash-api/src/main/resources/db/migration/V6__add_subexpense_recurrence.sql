ALTER TABLE subexpenses
    ADD COLUMN recurrence_frequency VARCHAR(16) NOT NULL DEFAULT 'NONE',
    ADD COLUMN recurrence_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN recurrence_index INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN series_id UUID;

ALTER TABLE subexpenses
    ADD CONSTRAINT chk_subexpenses_recurrence_frequency
    CHECK (recurrence_frequency IN ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY')),
    ADD CONSTRAINT chk_subexpenses_recurrence_count
    CHECK (recurrence_count >= 0 AND recurrence_count <= 120),
    ADD CONSTRAINT chk_subexpenses_recurrence_index
    CHECK (recurrence_index >= 0);

CREATE INDEX idx_subexpenses_series
    ON subexpenses (series_id);