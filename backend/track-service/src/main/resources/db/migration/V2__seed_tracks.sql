-- ============================================================
-- Dev seed data (2026-08-29)
-- Fixed UUIDs must match playlist-service V3__seed_playlists.sql
-- (membership references these track ids across DB boundaries).
-- audio_url points at the streaming endpoint; the actual audio
-- objects are seeded into MinIO at boot by TrackAudioSeedInitializer.
-- ============================================================

INSERT INTO tracks (id, title, artist, album, duration_ms, artwork_url, audio_url, created_at, updated_at) VALUES
  ('20000000-0000-4000-8000-000000000001', 'Play It Safe', 'Julia Wolf', 'Girls In Purgatory (Full of Grace)', 159000, '/figma/happy-hits.png', '/api/v1/tracks/20000000-0000-4000-8000-000000000001/audio', '2026-03-03T00:00:00Z', '2026-03-03T00:00:00Z'),
  ('20000000-0000-4000-8000-000000000002', 'In the Shape of a Dream', 'ayokay', 'In the Shape of a Dream', 132000, NULL, '/api/v1/tracks/20000000-0000-4000-8000-000000000002/audio', '2026-03-03T00:00:00Z', '2026-03-03T00:00:00Z'),
  ('20000000-0000-4000-8000-000000000003', 'Free Spirit', 'Khalid', 'Free Spirit (Explicit)', 182000, NULL, '/api/v1/tracks/20000000-0000-4000-8000-000000000003/audio', '2026-03-03T00:00:00Z', '2026-03-03T00:00:00Z'),
  ('20000000-0000-4000-8000-000000000004', 'Vacation', 'Stockholm Black', 'Vacation', 265000, NULL, '/api/v1/tracks/20000000-0000-4000-8000-000000000004/audio', '2026-03-03T00:00:00Z', '2026-03-03T00:00:00Z'),
  ('20000000-0000-4000-8000-000000000005', 'Same Old', 'Efraïm Leo', 'Same Old', 176000, NULL, '/api/v1/tracks/20000000-0000-4000-8000-000000000005/audio', '2026-03-03T00:00:00Z', '2026-03-03T00:00:00Z'),
  ('20000000-0000-4000-8000-000000000006', 'A Moment Apart', 'ODESZA', 'A Moment Apart', 234000, NULL, '/api/v1/tracks/20000000-0000-4000-8000-000000000006/audio', '2026-03-03T00:00:00Z', '2026-03-03T00:00:00Z');