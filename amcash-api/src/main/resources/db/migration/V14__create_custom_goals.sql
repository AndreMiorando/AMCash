CREATE TABLE custom_goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    target_amount NUMERIC(19, 2) NOT NULL,
    period VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_custom_goals_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_custom_goals_user UNIQUE (user_id),
    CONSTRAINT chk_custom_goals_target_amount CHECK (target_amount > 0),
    CONSTRAINT chk_custom_goals_period CHECK (period IN ('DAILY', 'WEEKLY', 'MONTHLY'))
);
