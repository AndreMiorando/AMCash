CREATE TABLE financial_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    entry_type VARCHAR(16) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    due_date DATE NOT NULL,
    recurrence_frequency VARCHAR(16) NOT NULL,
    recurrence_count INTEGER NOT NULL DEFAULT 0 CHECK (recurrence_count BETWEEN 0 AND 120),
    recurrence_index INTEGER NOT NULL DEFAULT 0 CHECK (recurrence_index >= 0),
    series_id UUID,
    has_subexpenses BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_financial_entries_user_due_date
    ON financial_entries (user_id, due_date);
CREATE INDEX idx_financial_entries_series
    ON financial_entries (series_id);

CREATE TABLE subexpenses (
    id UUID PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES financial_entries (id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    installment_description VARCHAR(80),
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_subexpenses_entry ON subexpenses (entry_id);
