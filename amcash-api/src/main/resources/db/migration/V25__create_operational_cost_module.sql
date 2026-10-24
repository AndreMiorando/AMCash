ALTER TABLE vehicles ADD COLUMN diesel_consumption NUMERIC(7, 2);

CREATE TABLE operational_cost_configurations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL UNIQUE REFERENCES vehicles(id) ON DELETE CASCADE,
    monthly_mileage NUMERIC(12, 2) NOT NULL,
    fuel_type VARCHAR(20) NOT NULL,
    fuel_price NUMERIC(19, 4) NOT NULL,
    include_finance BOOLEAN NOT NULL DEFAULT FALSE,
    annual_ipva NUMERIC(19, 2) NOT NULL DEFAULT 0,
    annual_fees NUMERIC(19, 2) NOT NULL DEFAULT 0,
    insurance_amount NUMERIC(19, 2),
    insurance_period VARCHAR(10),
    maintenance_method VARCHAR(16) NOT NULL,
    annual_maintenance_estimate NUMERIC(19, 2),
    include_depreciation BOOLEAN NOT NULL DEFAULT FALSE,
    current_vehicle_value NUMERIC(19, 2),
    annual_depreciation_rate NUMERIC(7, 4),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_operational_mileage CHECK (monthly_mileage > 0),
    CONSTRAINT ck_operational_fuel_price CHECK (fuel_price > 0),
    CONSTRAINT ck_operational_non_negative CHECK (annual_ipva >= 0 AND annual_fees >= 0 AND (insurance_amount IS NULL OR insurance_amount >= 0) AND (annual_maintenance_estimate IS NULL OR annual_maintenance_estimate >= 0) AND (current_vehicle_value IS NULL OR current_vehicle_value > 0) AND (annual_depreciation_rate IS NULL OR annual_depreciation_rate > 0))
);
CREATE INDEX idx_operational_cost_user ON operational_cost_configurations(user_id);

CREATE TABLE operational_cost_custom_fixed_costs (
    configuration_id UUID NOT NULL REFERENCES operational_cost_configurations(id) ON DELETE CASCADE,
    item_order INTEGER NOT NULL,
    title VARCHAR(60) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    period VARCHAR(10) NOT NULL,
    PRIMARY KEY (configuration_id, item_order),
    CONSTRAINT ck_operational_custom_amount CHECK (amount >= 0)
);

CREATE TABLE operational_cost_maintenance_selections (
    configuration_id UUID NOT NULL REFERENCES operational_cost_configurations(id) ON DELETE CASCADE,
    maintenance_id UUID NOT NULL REFERENCES vehicle_maintenances(id) ON DELETE CASCADE,
    PRIMARY KEY (configuration_id, maintenance_id)
);
