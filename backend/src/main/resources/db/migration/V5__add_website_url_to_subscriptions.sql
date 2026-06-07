-- Adds an optional website URL to subscriptions.
-- This allows the frontend to open the subscription provider's website from the detail section.

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS website_url VARCHAR(500);