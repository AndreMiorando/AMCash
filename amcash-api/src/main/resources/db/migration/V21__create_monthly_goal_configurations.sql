CREATE TABLE monthly_goal_configurations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    include_expenses BOOLEAN NOT NULL,
    additional_amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_monthly_goal_configurations_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_monthly_goal_configurations_user UNIQUE (user_id),
    CONSTRAINT chk_monthly_goal_configurations_additional_amount CHECK (additional_amount >= 0)
);

-- A meta personalizada que estava ativa vira uma meta mensal somente com adicional.
-- Metas personalizadas inativas permanecem na tabela legada, sem alterar o modo de despesas vigente.
INSERT INTO monthly_goal_configurations
    (id, user_id, include_expenses, additional_amount, created_at, updated_at)
SELECT id, user_id, FALSE, target_amount, created_at, updated_at
FROM custom_goals
WHERE tracked_on_home = TRUE;
