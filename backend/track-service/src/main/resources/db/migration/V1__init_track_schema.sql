-- Track catalog: metadata only for now. audio_url stays NULL until MinIO
-- upload/streaming lands (track-service phase 2) — column reserved from day one.
CREATE TABLE tracks (
    id          UUID PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    artist      VARCHAR(255) NOT NULL,
    album       VARCHAR(255),
    duration_ms BIGINT       NOT NULL CHECK (duration_ms >= 0),
    artwork_url VARCHAR(2048),
    audio_url   VARCHAR(2048),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);