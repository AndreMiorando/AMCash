CREATE TABLE work_earning_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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

INSERT INTO work_earning_sources (user_id, name, system_key, active, sort_order, created_at, updated_at)
SELECT id, 'Uber', 'UBER', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users;

INSERT INTO work_earning_sources (user_id, name, system_key, active, sort_order, created_at, updated_at)
SELECT id, '99', 'NINETY_NINE', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users;

INSERT INTO work_earning_sources (user_id, name, system_key, active, sort_order, created_at, updated_at)
SELECT historical.user_id, historical.name, NULL, TRUE, CAST(historical.position + 1 AS INTEGER),
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT day.user_id,
           MIN(TRIM(earning.source)) AS name,
           ROW_NUMBER() OVER (
               PARTITION BY day.user_id
               ORDER BY LOWER(TRIM(earning.source))
           ) AS position
    FROM work_earnings earning
    JOIN work_days day ON day.id = earning.work_day_id
    WHERE LOWER(TRIM(earning.source)) NOT IN ('uber', '99')
    GROUP BY day.user_id, LOWER(TRIM(earning.source))
) historical;
