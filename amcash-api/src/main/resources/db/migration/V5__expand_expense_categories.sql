ALTER TABLE financial_entries
    DROP CONSTRAINT chk_financial_entries_category;

ALTER TABLE financial_entries
    ADD CONSTRAINT chk_financial_entries_category
    CHECK (category IN (
        'FOOD', 'HOUSING', 'TRANSPORTATION', 'HEALTH', 'EDUCATION',
        'CLOTHING', 'LEISURE', 'BILLS_AND_SERVICES', 'SUBSCRIPTIONS',
        'CREDIT_CARD', 'LOANS_AND_FINANCING', 'FINANCIAL', 'INSURANCE',
        'TAXES', 'FAMILY', 'PETS', 'PERSONAL_CARE', 'TRAVEL', 'SHOPPING',
        'OTHER_EXPENSE', 'SALARY', 'FREELANCE', 'BUSINESS',
        'INVESTMENT_INCOME', 'RENTAL_INCOME', 'BENEFITS', 'REFUND',
        'GIFT', 'OTHER_INCOME'
    ));