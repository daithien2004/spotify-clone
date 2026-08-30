-- TOTP 2FA (local accounts) + profile update support
ALTER TABLE users
    ADD COLUMN totp_secret   VARCHAR(255),
    ADD COLUMN totp_enabled  BOOLEAN NOT NULL DEFAULT FALSE;