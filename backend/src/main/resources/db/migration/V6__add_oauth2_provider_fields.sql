-- V6: Add OAuth2 provider fields to support Google login
-- password is now nullable for OAuth2 users who don't have a local password

ALTER TABLE users
    ALTER COLUMN password DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS provider VARCHAR(50) NOT NULL DEFAULT 'local',
    ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);

-- Unique constraint to prevent duplicate OAuth2 accounts
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_provider_provider_id
    ON users(provider, provider_id) WHERE provider_id IS NOT NULL;
