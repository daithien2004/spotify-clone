/**
 * Adapter giữa shape backend (playlist/track service DTOs, đã unwrap envelope)
 * và domain types FE (TrackItem / Playlist). Đặt toàn bộ mapping ở đây để
 * components/services không nhìn thấy chi tiết payload backend.
 */
import type { Playlist, TrackItem } from "./musicTypes";
import type { Track } from "@/hooks/usePlayerStore";

/** GET /tracks (track-service TrackResponse). */
export interface TrackResponse {
  id: string;
  title: string;
  artist: string;
  album: string;
  /** milliseconds — UI dùng giây. */
  durationMs: number;
  artworkUrl: string | null;
  audioUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

/** GET /playlists/{id} (playlist-service PlaylistResponse). */
export interface PlaylistResponse {
  id: string;
  title: string;
  description: string | null;
  ownerName: string;
  coverUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

/** GET /playlists (list) — sidebar chỉ cần vài field. */
export interface PlaylistSummaryResponse {
  id: string;
  title: string;
  ownerName: string;
  coverUrl: string | null;
}

/** GET /playlists/{id}/tracks (membership rows, có lexoRank để join theo thứ tự). */
export interface PlaylistTrackResponse {
  id: string;
  trackId: string;
  lexoRank: string;
  addedAt: string;
  updatedAt: string;
}

export const DEFAULT_COVER = "/figma/chill-mix.png";

/** "2026-03-03T00:00:00Z" → "Mar 3, 2026"; input rác → "". */
export function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  return d.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function trackResponseToTrackItem(t: TrackResponse): TrackItem {
  return {
    id: t.id,
    title: t.title,
    artist: t.artist,
    album: t.album,
    durationSec: Math.round(t.durationMs / 1000),
    dateAdded: formatDate(t.createdAt),
    coverUrl: t.artworkUrl ?? undefined,
    audioUrl: t.audioUrl ?? undefined,
  };
}

/** TrackItem (liệt kê) → Track (store player). */
export function toPlayerTrack(t: TrackItem): Track {
  return {
    id: t.id,
    title: t.title,
    artist: t.artist,
    imageUrl: t.coverUrl ?? DEFAULT_COVER,
    duration: t.durationSec,
    audioUrl: t.audioUrl,
  };
}

export function playlistResponseToPlaylist(p: PlaylistResponse): Playlist {
  return {
    id: p.id,
    title: p.title,
    type: "PUBLIC PLAYLIST",
    description: p.description ?? "",
    owner: p.ownerName,
    coverUrl: p.coverUrl ?? DEFAULT_COVER,
    gradient: "from-genre-chill via-genre-chill/40",
    songs: [],
    totalSongs: 0,
    totalDurationLabel: "",
  };
}

/** "2hr 01 min" — nhãn tổng thời lượng cho header playlist. */
export function formatDurationLabel(totalSec: number): string {
  if (totalSec <= 0) return "";
  const hours = Math.floor(totalSec / 3600);
  const minutes = Math.floor((totalSec % 3600) / 60);
  return hours > 0 ? `${hours}hr ${minutes} min` : `${minutes} min`;
}