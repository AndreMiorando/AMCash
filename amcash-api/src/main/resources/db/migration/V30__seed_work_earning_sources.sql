INSERT INTO work_earning_sources
    (id, user_id, name, system_key, active, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), users.id, 'Uber', 'UBER', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
ON CONFLICT DO NOTHING;

INSERT INTO work_earning_sources
    (id, user_id, name, system_key, active, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), users.id, '99', 'NINETY_NINE', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users
ON CONFLICT DO NOTHING;

INSERT INTO work_earning_sources
    (id, user_id, name, system_key, active, sort_order, created_at, updated_at)
SELECT gen_random_uuid(), historical.user_id, historical.name, NULL, TRUE,
       CAST(historical.position + 1 AS INTEGER), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT day.user_id,
           MIN(TRIM(earning.source)) AS name,
           ROW_NUMBER() OVER (
               PARTITION BY day.user_id
               ORDER BY LOWER(TRIM(earning.source))
           ) AS position
    FROM work_earnings earning
    JOIN work_days day ON day.id = earning.work_day_id
    WHERE TRIM(earning.source) <> ''
      AND LOWER(TRIM(earning.source)) NOT IN ('uber', '99')
    GROUP BY day.user_id, LOWER(TRIM(earning.source))
) historical
ON CONFLICT DO NOTHING;
