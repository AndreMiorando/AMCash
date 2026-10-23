create table financial_notifications (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    entry_id uuid not null references financial_entries(id) on delete cascade,
    alert_type varchar(24) not null,
    created_at timestamp with time zone not null,
    read_at timestamp with time zone,
    active boolean not null default true,
    constraint uk_financial_notifications_user_entry unique (user_id, entry_id)
);

create index idx_financial_notifications_user_active
    on financial_notifications (user_id, active);
