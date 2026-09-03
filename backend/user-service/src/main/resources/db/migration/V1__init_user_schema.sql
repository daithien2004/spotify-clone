-- ============================================================
-- User schema (database-per-service: user_db)
-- users = projection từ Kafka User.Registered (auth-service)
-- user_follows = social follow relation (follower → followee)
-- ============================================================

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    avatar_url VARCHAR(1024),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE user_follows (
    id UUID PRIMARY KEY,
    follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    -- Idempotent follow: ràng buộc duy nhất (follower, followee) chặn trùng.
    CONSTRAINT uq_follower_followee UNIQUE (follower_id, followee_id),
    -- Không tự-follow: chặn follower == followee ở tầng DB.
    CONSTRAINT chk_no_self_follow CHECK (follower_id <> followee_id)
);

-- Lookup followers/following theo hướng query.
CREATE INDEX idx_follows_follower ON user_follows (follower_id, followee_id);
CREATE INDEX idx_follows_followee ON user_follows (followee_id, follower_id);
