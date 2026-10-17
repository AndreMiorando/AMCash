CREATE TABLE work_costs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    cost_date DATE NOT NULL,
    category VARCHAR(24) NOT NULL CHECK (category IN (
        'FUEL', 'MAINTENANCE', 'PARKING', 'TOLL', 'WASH', 'OTHER'
    )),
    description VARCHAR(160),
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_work_costs_user_date ON work_costs (user_id, cost_date);

INSERT INTO work_costs (id, user_id, cost_date, category, description, amount, created_at, updated_at)
SELECT GEN_RANDOM_UUID(), user_id, work_date, 'FUEL', 'Combustível', fuel_cost, created_at, updated_at
FROM work_days
WHERE fuel_cost > 0;

INSERT INTO work_costs (id, user_id, cost_date, category, description, amount, created_at, updated_at)
SELECT GEN_RANDOM_UUID(), user_id, work_date, 'OTHER', 'Outros custos', other_costs, created_at, updated_at
FROM work_days
WHERE other_costs > 0;

UPDATE work_days SET fuel_cost = 0, other_costs = 0;
