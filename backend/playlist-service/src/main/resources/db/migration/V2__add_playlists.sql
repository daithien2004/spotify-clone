-- ============================================================
-- Playlist metadata (2026-08-29)
-- Add the playlists table so FE header / sidebar can join real
-- metadata instead of mock data. track membership stays in
-- playlist_tracks (existing V1).
-- ============================================================

CREATE TABLE playlists (
    id          UUID PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    owner_name  VARCHAR(255) NOT NULL,
    cover_url   VARCHAR(2048),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);