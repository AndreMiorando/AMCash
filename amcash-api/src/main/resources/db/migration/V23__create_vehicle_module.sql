CREATE TABLE vehicles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vehicle_type VARCHAR(16) NOT NULL,
    brand VARCHAR(60) NOT NULL,
    model VARCHAR(80) NOT NULL,
    manufacture_year INTEGER NOT NULL,
    model_year INTEGER NOT NULL,
    version VARCHAR(80),
    nickname VARCHAR(60),
    license_plate VARCHAR(10),
    fuel_type VARCHAR(20) NOT NULL,
    current_mileage NUMERIC(12, 1) NOT NULL,
    mileage_reading_date DATE NOT NULL,
    gasoline_consumption NUMERIC(7, 2),
    ethanol_consumption NUMERIC(7, 2),
    electric_consumption NUMERIC(7, 2),
    acquisition_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_vehicle_years CHECK (manufacture_year BETWEEN 1900 AND 2200 AND model_year BETWEEN 1900 AND 2201),
    CONSTRAINT ck_vehicle_mileage CHECK (current_mileage >= 0)
);
CREATE INDEX idx_vehicles_user ON vehicles(user_id);

CREATE TABLE vehicle_finances (
    vehicle_id UUID PRIMARY KEY REFERENCES vehicles(id) ON DELETE CASCADE,
    financial_status VARCHAR(20) NOT NULL,
    monthly_amount NUMERIC(19, 2),
    total_installments INTEGER,
    paid_installments INTEGER,
    next_due_date DATE,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE vehicle_odometer_readings (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    mileage NUMERIC(12, 1) NOT NULL,
    reading_date DATE NOT NULL,
    correction BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_odometer_mileage CHECK (mileage >= 0)
);
CREATE INDEX idx_odometer_vehicle ON vehicle_odometer_readings(vehicle_id, reading_date DESC, created_at DESC);

CREATE TABLE vehicle_maintenances (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    maintenance_type VARCHAR(40) NOT NULL,
    custom_name VARCHAR(80),
    maintenance_date DATE NOT NULL,
    service_mileage NUMERIC(12, 1) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    description VARCHAR(300),
    next_mileage NUMERIC(12, 1),
    next_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_maintenance_mileage CHECK (service_mileage >= 0 AND (next_mileage IS NULL OR next_mileage > service_mileage)),
    CONSTRAINT ck_maintenance_amount CHECK (amount >= 0),
    CONSTRAINT ck_maintenance_next_date CHECK (next_date IS NULL OR next_date >= maintenance_date)
);
CREATE INDEX idx_maintenance_vehicle ON vehicle_maintenances(vehicle_id, maintenance_date DESC, created_at DESC);

CREATE TABLE vehicle_photos (
    vehicle_id UUID PRIMARY KEY REFERENCES vehicles(id) ON DELETE CASCADE,
    content_type VARCHAR(32) NOT NULL,
    content BYTEA NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE vehicle_maintenance_alerts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    maintenance_id UUID NOT NULL UNIQUE REFERENCES vehicle_maintenances(id) ON DELETE CASCADE,
    alert_type VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_vehicle_alert_user ON vehicle_maintenance_alerts(user_id, active);
