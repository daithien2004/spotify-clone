/**
 * Playlist API (playlist-service) — GET playlists / {id} / {id}/tracks.
 * Mọi response từ backend đều là envelope {success, data, ...}; service này
 * unwrap `.data` rồi adapt về domain types FE.
 */
import { api, type ApiResponse, unwrap } from "@/lib/api-client";
import type { Playlist } from "@/lib/musicTypes";
import {
  DEFAULT_COVER,
  playlistResponseToPlaylist,
  type PlaylistResponse,
  type PlaylistSummaryResponse,
  type PlaylistTrackResponse,
} from "@/lib/adapters";

/** Sidebar cần ít field — không kéo cả dto playlist đầy đủ. */
export interface PlaylistSummaryItem {
  id: string;
  title: string;
  owner: string;
  coverUrl: string;
}

export function toSummaryItem(p: PlaylistSummaryResponse): PlaylistSummaryItem {
  return {
    id: p.id,
    title: p.title,
    owner: p.ownerName,
    coverUrl: p.coverUrl ?? DEFAULT_COVER,
  };
}

export class PlaylistService {
  static async getPlaylist(id: string): Promise<Playlist> {
    const envelope = await api.get<ApiResponse<PlaylistResponse>>(`/playlists/${id}`);
    return playlistResponseToPlaylist(unwrap(envelope));
  }

  static async listPlaylists(): Promise<PlaylistSummaryItem[]> {
    const envelope = await api.get<ApiResponse<PlaylistSummaryResponse[]>>("/playlists");
    return unwrap(envelope).map(toSummaryItem);
  }

  static async getPlaylistTracks(id: string): Promise<PlaylistTrackResponse[]> {
    const envelope = await api.get<ApiResponse<PlaylistTrackResponse[]>>(`/playlists/${id}/tracks`);
    return unwrap(envelope);
  }
}