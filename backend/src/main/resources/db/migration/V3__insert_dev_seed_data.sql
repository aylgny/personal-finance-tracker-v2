INSERT INTO app_users (id, name, email, password_hash)
VALUES
    (1, 'Demo User', 'demo@subtrack.com', '$2a$12$demoPasswordHashPlaceholder')
ON CONFLICT (email) DO NOTHING;

INSERT INTO currencies (id, user_id, code, symbol, name, exchange_rate)
VALUES
    (1, 1, 'TRY', '₺', 'Turkish Lira', 1.00000000),
    (2, 1, 'USD', '$', 'US Dollar', 32.00000000),
    (3, 1, 'EUR', '€', 'Euro', 35.00000000)
ON CONFLICT (user_id, code) DO NOTHING;

INSERT INTO categories (id, user_id, name, sort_order)
VALUES
    (1, 1, 'Streaming', 1),
    (2, 1, 'Music', 2),
    (3, 1, 'Cloud Storage', 3),
    (4, 1, 'Productivity', 4)
ON CONFLICT DO NOTHING;

INSERT INTO payment_methods (id, user_id, name, enabled, sort_order)
VALUES
    (1, 1, 'Credit Card', TRUE, 1),
    (2, 1, 'Bank Transfer', TRUE, 2)
ON CONFLICT DO NOTHING;

INSERT INTO subscriptions (
    id,
    user_id,
    category_id,
    payment_method_id,
    currency_id,
    name,
    provider,
    price,
    billing_cycle,
    status,
    start_date,
    next_payment_date,
    auto_renew,
    notify_enabled,
    notify_days_before,
    notes
)
VALUES
    (
        1,
        1,
        1,
        1,
        1,
        'Netflix',
        'Netflix',
        229.99,
        'MONTHLY',
        'ACTIVE',
        CURRENT_DATE - INTERVAL '6 months',
        CURRENT_DATE + INTERVAL '5 days',
        TRUE,
        TRUE,
        3,
        'Demo streaming subscription'
    ),
    (
        2,
        1,
        2,
        1,
        1,
        'Spotify',
        'Spotify',
        59.99,
        'MONTHLY',
        'ACTIVE',
        CURRENT_DATE - INTERVAL '1 year',
        CURRENT_DATE + INTERVAL '12 days',
        TRUE,
        TRUE,
        3,
        'Demo music subscription'
    ),
    (
        3,
        1,
        3,
        2,
        2,
        'Google One',
        'Google',
        19.99,
        'YEARLY',
        'ACTIVE',
        CURRENT_DATE - INTERVAL '3 months',
        CURRENT_DATE + INTERVAL '45 days',
        TRUE,
        TRUE,
        7,
        'Demo cloud storage subscription'
    )
ON CONFLICT DO NOTHING;

SELECT setval('app_users_id_seq', COALESCE((SELECT MAX(id) FROM app_users), 1));
SELECT setval('currencies_id_seq', COALESCE((SELECT MAX(id) FROM currencies), 1));
SELECT setval('categories_id_seq', COALESCE((SELECT MAX(id) FROM categories), 1));
SELECT setval('payment_methods_id_seq', COALESCE((SELECT MAX(id) FROM payment_methods), 1));
SELECT setval('subscriptions_id_seq', COALESCE((SELECT MAX(id) FROM subscriptions), 1));