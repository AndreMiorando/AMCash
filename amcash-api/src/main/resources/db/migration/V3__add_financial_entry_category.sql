ALTER TABLE financial_entries
    ADD COLUMN category VARCHAR(32);

UPDATE financial_entries
SET category = CASE
    WHEN entry_type = 'INCOME' THEN 'OTHER_INCOME'
    ELSE 'OTHER_EXPENSE'
END;

ALTER TABLE financial_entries
    ALTER COLUMN category SET NOT NULL;

ALTER TABLE financial_entries
    ADD CONSTRAINT chk_financial_entries_category
    CHECK (category IN (
        'FOOD', 'HOUSING', 'TRANSPORTATION', 'HEALTH', 'EDUCATION',
        'CLOTHING', 'LEISURE', 'BILLS_AND_SERVICES', 'FINANCIAL', 'TAXES',
        'FAMILY', 'PETS', 'SHOPPING', 'OTHER_EXPENSE', 'SALARY', 'FREELANCE',
        'BUSINESS', 'INVESTMENT_INCOME', 'RENTAL_INCOME', 'BENEFITS', 'REFUND',
        'GIFT', 'OTHER_INCOME'
    ));

CREATE INDEX idx_financial_entries_user_category
    ON financial_entries (user_id, category);
