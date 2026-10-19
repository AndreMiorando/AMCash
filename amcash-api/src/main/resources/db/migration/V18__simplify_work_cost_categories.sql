UPDATE work_costs
SET category = 'OTHER'
WHERE category IN ('PARKING', 'MARKET');

ALTER TABLE work_costs
    DROP CONSTRAINT work_costs_category_check;

ALTER TABLE work_costs
    ADD CONSTRAINT work_costs_category_check CHECK (category IN (
        'FUEL', 'MAINTENANCE', 'TOLL', 'WASH', 'FOOD', 'OTHER'
    ));
