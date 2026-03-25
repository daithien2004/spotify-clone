-- Thêm các trường bảo mật vào bảng users cho Authentication nâng cao
-- is_verified: Xác thực email
-- totp_secret: Bí mật TOTP cho 2FA
-- is_2fa_enabled: 2FA đã được bật chưa
-- failed_login_attempts: Theo dõi số lần đăng nhập thất bại (chống Brute Force)
-- locked_until: Tài khoản bị khoá đến thời điểm nào

ALTER TABLE users
    ADD COLUMN is_verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN totp_secret        VARCHAR(255),
    ADD COLUMN is_2fa_enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN failed_login_attempts INTEGER   NOT NULL DEFAULT 0,
    ADD COLUMN locked_until       TIMESTAMPTZ;

-- Thêm trường family_id vào refresh_tokens để nhóm các token trong cùng một phiên đăng nhập
-- Dùng để phát hiện Refresh Token Reuse: nếu token cũ trong cùng family được dùng lại,
-- ta sẽ revoke toàn bộ family (tức là force logout tất cả thiết bị của user đó).
ALTER TABLE refresh_tokens
    ADD COLUMN family_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN replaced_by VARCHAR(255),
    ADD COLUMN ip_address  VARCHAR(100),
    ADD COLUMN user_agent  VARCHAR(512);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users(locked_until);
