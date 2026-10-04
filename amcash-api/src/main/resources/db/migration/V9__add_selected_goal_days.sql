ALTER TABLE goal_month_preferences
    ADD COLUMN selected_days_mask INTEGER NOT NULL DEFAULT 0;

ALTER TABLE goal_month_preferences
    ADD CONSTRAINT chk_goal_month_preferences_selected_days_mask
    CHECK (selected_days_mask BETWEEN 0 AND 2147483647);
