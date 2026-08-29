-- ============================================================
-- Auth schema (consolidated baseline, 2026-08-29)
-- Gộp các migration cũ V1/V2/V3/V6 thành 1 file vì đã chuyển
-- sang database-per-service. `security_tokens` (V4 cũ) đã bỏ:
--   app dùng Redis làm store (RedisSecurityTokenAdapter), bảng
--   không còn JPA entity nào map tới.
-- ============================================================

-- ===================== users =====================
CREATE TABLE users (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                VARCHAR(255) NOT NULL UNIQUE,
    -- password nullable: user OAuth2 (Google) không có local password
    password             VARCHAR(255),
    display_name         VARCHAR(255) NOT NULL,
    avatar_url           VARCHAR(255),
    -- Audit + security fields
    is_verified          BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER     NOT NULL DEFAULT 0,
    locked_until         TIMESTAMPTZ,
    -- OAuth2 provider ('local' hoặc tên provider)
    provider             VARCHAR(50)  NOT NULL DEFAULT 'local',
    provider_id          VARCHAR(255),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Ngăn duplicate OAuth2 account theo (provider, provider_id)
CREATE UNIQUE INDEX idx_users_provider_provider_id
    ON users(provider, provider_id) WHERE provider_id IS NOT NULL;

CREATE INDEX idx_users_locked_until ON users(locked_until);

-- ===================== user_roles =====================
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role     VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- ===================== refresh_tokens =====================
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    -- family_id: nhóm token cùng phiên → detect Refresh Token Reuse
    family_id   UUID        NOT NULL DEFAULT gen_random_uuid(),
    replaced_by VARCHAR(255),
    ip_address  VARCHAR(100),
    user_agent  VARCHAR(512),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);