ALTER TABLE financial_entries
    ADD COLUMN payment_method VARCHAR(24),
    ADD COLUMN notes VARCHAR(500);
