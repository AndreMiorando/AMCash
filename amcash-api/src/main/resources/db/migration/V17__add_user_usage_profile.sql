ALTER TABLE users
    ADD COLUMN usage_profile VARCHAR(32);

ALTER TABLE users
    ADD CONSTRAINT users_usage_profile_check CHECK (usage_profile IN (
        'PERSONAL', 'RIDE_DRIVER', 'DELIVERY'
    ));
