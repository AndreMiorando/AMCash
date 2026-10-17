CREATE TABLE work_days (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    kilometers NUMERIC(12, 2) NOT NULL CHECK (kilometers >= 0),
    fuel_cost NUMERIC(19, 2) NOT NULL CHECK (fuel_cost >= 0),
    other_costs NUMERIC(19, 2) NOT NULL CHECK (other_costs >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_work_days_user_date UNIQUE (user_id, work_date)
);

CREATE INDEX idx_work_days_user_date ON work_days (user_id, work_date);

CREATE TABLE work_earnings (
    id UUID PRIMARY KEY,
    work_day_id UUID NOT NULL REFERENCES work_days (id) ON DELETE CASCADE,
    source VARCHAR(80) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount >= 0),
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_work_earnings_day ON work_earnings (work_day_id, sort_order);
