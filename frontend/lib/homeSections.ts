/**
 * Build các section cho Home feed từ dữ liệu thật (playlist-service + track-service).
 * Pure function — tách riêng để dễ test (TDD) và để HomeFeed chỉ lo render.
 */
import { DEFAULT_COVER, type TrackResponse } from "@/lib/adapters";
import type { PlaylistSummaryItem } from "@/services/api/playlistService";

/** Shape section của Home feed (giống mock HOME_SECTIONS). */
export interface HomeSection {
  title: string;
  seeAll?: boolean;
  items: Array<{
    id: string;
    title: string;
    description: string;
    imageUrl: string;
    type: string;
    variant?: "track" | "artist";
  }>;
}

function playlistSection(playlists: PlaylistSummaryItem[]): HomeSection {
  return {
    title: "Your playlists",
    seeAll: true,
    items: playlists.map((p) => ({
      id: p.id,
      title: p.title,
      description: p.owner || "Playlist",
      imageUrl: p.coverUrl || DEFAULT_COVER,
      type: "Playlist",
    })),
  };
}

function trackSection(tracks: TrackResponse[]): HomeSection {
  return {
    title: "Trending tracks",
    items: tracks.map((t) => ({
      id: t.id,
      title: t.title,
      description: t.artist,
      imageUrl: t.artworkUrl ?? DEFAULT_COVER,
      type: "Track",
    })),
  };
}

export function buildHomeSections(
  playlists: PlaylistSummaryItem[],
  tracks: TrackResponse[]
): HomeSection[] {
  const sections: HomeSection[] = [];
  if (playlists.length > 0) sections.push(playlistSection(playlists));
  if (tracks.length > 0) sections.push(trackSection(tracks));
  return sections;
}