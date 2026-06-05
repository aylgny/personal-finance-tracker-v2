CREATE TABLE currencies (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(3) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    exchange_rate NUMERIC(19,8) NOT NULL DEFAULT 1.00000000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_currencies_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_currencies_user_code
        UNIQUE (user_id, code)
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
);

CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_methods_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
);

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT,
    payment_method_id BIGINT,
    currency_id BIGINT NOT NULL,

    name VARCHAR(150) NOT NULL,
    provider VARCHAR(150),
    price NUMERIC(19,4) NOT NULL,
    billing_cycle VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    start_date DATE,
    next_payment_date DATE NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    notify_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notify_days_before INTEGER NOT NULL DEFAULT 3,

    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_subscriptions_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_subscriptions_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_subscriptions_payment_method
        FOREIGN KEY (payment_method_id)
        REFERENCES payment_methods(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_subscriptions_currency
        FOREIGN KEY (currency_id)
        REFERENCES currencies(id)
);

CREATE INDEX idx_currencies_user_code
    ON currencies(user_id, code);

CREATE INDEX idx_categories_user_sort_order
    ON categories(user_id, sort_order);

CREATE INDEX idx_payment_methods_user_enabled_sort_order
    ON payment_methods(user_id, enabled, sort_order);

CREATE INDEX idx_subscriptions_user_status_next_payment
    ON subscriptions(user_id, status, next_payment_date);

CREATE INDEX idx_subscriptions_user_category
    ON subscriptions(user_id, category_id);

CREATE INDEX idx_subscriptions_user_payment_method
    ON subscriptions(user_id, payment_method_id);

CREATE INDEX idx_subscriptions_next_payment_active_auto_renew
    ON subscriptions(next_payment_date)
    WHERE status = 'ACTIVE' AND auto_renew = TRUE;