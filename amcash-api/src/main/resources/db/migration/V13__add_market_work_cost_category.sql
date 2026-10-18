ALTER TABLE work_costs
    DROP CONSTRAINT work_costs_category_check;

ALTER TABLE work_costs
    ADD CONSTRAINT work_costs_category_check CHECK (category IN (
        'FUEL', 'MAINTENANCE', 'PARKING', 'TOLL', 'WASH', 'MARKET', 'OTHER'
    ));
