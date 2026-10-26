UPDATE operational_cost_configurations
SET maintenance_method = 'LAST_12_MONTHS'
WHERE maintenance_method = 'HISTORY';

DROP TABLE operational_cost_maintenance_selections;
