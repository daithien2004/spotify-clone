-- Bảng lưu các token dùng một lần cho: Email Verification, Password Reset
-- Thư viện Redis sẽ là nơi chính để lookup (TTL tự động hết hạn),
-- bảng này chỉ dùng để audit trail và xử lý edge case khi Redis restart.

CREATE TABLE security_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token       VARCHAR(512) NOT NULL UNIQUE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_type  VARCHAR(50) NOT NULL, -- 'EMAIL_VERIFICATION', 'PASSWORD_RESET', '2FA_SETUP'
    used_at     TIMESTAMPTZ,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_security_tokens_token ON security_tokens(token);
CREATE INDEX IF NOT EXISTS idx_security_tokens_user_id ON security_tokens(user_id);
