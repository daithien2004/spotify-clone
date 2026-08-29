-- ============================================================
-- Playlist schema (consolidated baseline, 2026-08-29)
-- Đổi tên từ V5 cũ → V1 vì database-per-service: mỗi service có
-- flyway history riêng, đánh số lại từ đầu theo từng service.
-- ============================================================

CREATE TABLE playlist_tracks (
    id UUID PRIMARY KEY,
    playlist_id UUID NOT NULL,
    track_id UUID NOT NULL,
    lexo_rank VARCHAR(128) NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_playlist_id_rank ON playlist_tracks (playlist_id, lexo_rank ASC);
