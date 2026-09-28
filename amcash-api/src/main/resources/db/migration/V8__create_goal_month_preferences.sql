CREATE TABLE goal_month_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    goal_year INTEGER NOT NULL,
    goal_month INTEGER NOT NULL,
    weekdays_mask INTEGER NOT NULL DEFAULT 127,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_goal_month_preferences_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_goal_month_preferences_user_period
        UNIQUE (user_id, goal_year, goal_month),
    CONSTRAINT chk_goal_month_preferences_month
        CHECK (goal_month BETWEEN 1 AND 12),
    CONSTRAINT chk_goal_month_preferences_weekdays_mask
        CHECK (weekdays_mask BETWEEN 1 AND 127)
);
