ALTER TABLE financial_entries
    ADD CONSTRAINT chk_financial_entries_name_length
        CHECK (char_length(regexp_replace(name, '[[:space:]]+-[[:space:]]+[0-9]+/[0-9]+$', '')) <= 40) NOT VALID,
    ADD CONSTRAINT chk_financial_entries_notes_length
        CHECK (notes IS NULL OR char_length(notes) <= 120) NOT VALID;

ALTER TABLE subexpenses
    ADD CONSTRAINT chk_subexpenses_name_length
        CHECK (char_length(name) <= 40) NOT VALID;

ALTER TABLE custom_goals
    ADD CONSTRAINT chk_custom_goals_name_length
        CHECK (char_length(name) <= 40) NOT VALID;

ALTER TABLE work_earnings
    ADD CONSTRAINT chk_work_earnings_source_length
        CHECK (char_length(source) <= 30) NOT VALID;

ALTER TABLE work_costs
    ADD CONSTRAINT chk_work_costs_description_length
        CHECK (description IS NULL OR char_length(description) <= 60) NOT VALID;
