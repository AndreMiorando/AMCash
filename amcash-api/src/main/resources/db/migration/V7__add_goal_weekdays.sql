ALTER TABLE users
    ADD COLUMN goal_weekdays_mask INTEGER NOT NULL DEFAULT 31;

ALTER TABLE users
    ADD CONSTRAINT chk_users_goal_weekdays_mask
    CHECK (goal_weekdays_mask BETWEEN 1 AND 127);
