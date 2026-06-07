-- Converts currencies, categories, and payment methods into global reference data.
-- These tables are shared by all users and are used for dropdown selections.

-- Step 1: Update subscriptions to point to the first matching duplicate currency by code.
WITH currency_duplicates AS (
    SELECT
        id,
        MIN(id) OVER (PARTITION BY code) AS keep_id
    FROM currencies
)
UPDATE subscriptions s
SET currency_id = d.keep_id
    FROM currency_duplicates d
WHERE s.currency_id = d.id
  AND d.id <> d.keep_id;

-- Step 2: Update subscriptions to point to the first matching duplicate category by name.
WITH category_duplicates AS (
    SELECT
        id,
        MIN(id) OVER (PARTITION BY name) AS keep_id
    FROM categories
)
UPDATE subscriptions s
SET category_id = d.keep_id
    FROM category_duplicates d
WHERE s.category_id = d.id
  AND d.id <> d.keep_id;

-- Step 3: Update subscriptions to point to the first matching duplicate payment method by name.
WITH payment_method_duplicates AS (
    SELECT
        id,
        MIN(id) OVER (PARTITION BY name) AS keep_id
    FROM payment_methods
)
UPDATE subscriptions s
SET payment_method_id = d.keep_id
    FROM payment_method_duplicates d
WHERE s.payment_method_id = d.id
  AND d.id <> d.keep_id;

-- Step 4: Remove duplicate reference records after subscriptions have been repointed.
DELETE FROM currencies c
    USING currencies duplicate
WHERE c.id > duplicate.id
  AND c.code = duplicate.code;

DELETE FROM categories c
    USING categories duplicate
WHERE c.id > duplicate.id
  AND c.name = duplicate.name;

DELETE FROM payment_methods p
    USING payment_methods duplicate
WHERE p.id > duplicate.id
  AND p.name = duplicate.name;

-- Step 5: Remove user ownership from reference tables.
ALTER TABLE currencies DROP COLUMN IF EXISTS user_id;
ALTER TABLE categories DROP COLUMN IF EXISTS user_id;
ALTER TABLE payment_methods DROP COLUMN IF EXISTS user_id;

-- Step 6: Seed global currencies.
INSERT INTO currencies (code, symbol, name, exchange_rate)
SELECT 'TRY', '₺', 'Turkish Lira', 1.00000000
    WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'TRY');

INSERT INTO currencies (code, symbol, name, exchange_rate)
SELECT 'USD', '$', 'US Dollar', 1.00000000
    WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'USD');

INSERT INTO currencies (code, symbol, name, exchange_rate)
SELECT 'EUR', '€', 'Euro', 1.00000000
    WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'EUR');

-- Step 7: Seed global categories.
INSERT INTO categories (name, sort_order)
SELECT 'Entertainment', 1
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Entertainment');

INSERT INTO categories (name, sort_order)
SELECT 'Music', 2
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Music');

INSERT INTO categories (name, sort_order)
SELECT 'Cloud Storage', 3
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Cloud Storage');

INSERT INTO categories (name, sort_order)
SELECT 'Education', 4
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Education');

INSERT INTO categories (name, sort_order)
SELECT 'Software', 5
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Software');

INSERT INTO categories (name, sort_order)
SELECT 'Other', 99
    WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Other');

-- Step 8: Seed global payment methods.
INSERT INTO payment_methods (name, enabled, sort_order)
SELECT 'Credit Card', true, 1
    WHERE NOT EXISTS (SELECT 1 FROM payment_methods WHERE name = 'Credit Card');

INSERT INTO payment_methods (name, enabled, sort_order)
SELECT 'Debit Card', true, 2
    WHERE NOT EXISTS (SELECT 1 FROM payment_methods WHERE name = 'Debit Card');

INSERT INTO payment_methods (name, enabled, sort_order)
SELECT 'Bank Transfer', true, 3
    WHERE NOT EXISTS (SELECT 1 FROM payment_methods WHERE name = 'Bank Transfer');

INSERT INTO payment_methods (name, enabled, sort_order)
SELECT 'Cash', true, 4
    WHERE NOT EXISTS (SELECT 1 FROM payment_methods WHERE name = 'Cash');

INSERT INTO payment_methods (name, enabled, sort_order)
SELECT 'Other', true, 99
    WHERE NOT EXISTS (SELECT 1 FROM payment_methods WHERE name = 'Other');