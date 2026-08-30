/**
 * Search API (search-service) — real full-text search behind the gateway.
 * FE SearchBar switches from the TRACK_INDEX mock to this (spec §7).
 */
import { api, type ApiResponse, unwrap } from "@/lib/api-client";

/** Search result item as returned by the backend (spec §4 fields). */
export interface SearchItem {
  id: string;
  title: string;
  artist: string;
  album: string;
  artworkUrl?: string;
  audioUrl?: string;
  durationMs?: number;
}

export class SearchApiService {
  /** GET /api/v1/search/tracks?q=&limit= → unwrapped SearchItem[]. */
  static async search(query: string, limit = 10): Promise<SearchItem[]> {
    const q = query.trim();
    if (!q) return [];
    const envelope = await api.get<ApiResponse<SearchItem[]>>("/search/tracks", {
      params: { q, limit },
    });
    return unwrap(envelope);
  }
}