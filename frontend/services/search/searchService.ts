import type { TrackItem } from "@/lib/musicTypes";

/** Kết quả bài hát khi tìm kiếm (khớp row 60×60 của Figma 13:2). */
export interface SearchResult {
  id: string;
  title: string;
  artist: string;
  album: string;
  coverUrl?: string;
}

/** Gợi ý từ title/artist/album — click để điền vào ô search. */
export interface SearchSuggestion {
  id: string;
  text: string;
}

const SUGGESTION_LIMIT = 5;

function normalize(value: string): string {
  return value.trim().toLowerCase();
}

/**
 * Contract search: backend Search-service (Backlog). Hiện tại mock adapter
 * lọc trong-nhớ từ track index (thay axios → api.get khi backend có).
 */
export class SearchService {
  static searchTracks(query: string, index: ReadonlyArray<TrackItem>): SearchResult[] {
    const q = normalize(query);
    if (!q) return [];

    return index
      .filter((t) => {
        const haystack = `${t.title} ${t.artist} ${t.album}`.toLowerCase();
        return haystack.includes(q);
      })
      .map(({ id, title, artist, album, coverUrl }) => ({ id, title, artist, album, coverUrl }));
  }

  static searchSuggestions(query: string, index: ReadonlyArray<TrackItem>): SearchSuggestion[] {
    const q = normalize(query);
    if (!q) return [];

    const seen = new Set<string>();
    const result: SearchSuggestion[] = [];

    // Ưu tiên prefix-match (title/artist bắt đầu bằng query) trước contains-match.
    const byPriority = (value: string) => {
      const text = normalize(value);
      return text.startsWith(q) ? 0 : 1;
    };

    const push = (value: string, id: string) => {
      const text = normalize(value);
      if (!text.includes(q) || seen.has(text)) return;
      seen.add(text);
      result.push({ id: `${id}-${text}`, text: value });
    };

    for (const t of index) {
      push(t.title, t.id);
      push(t.artist, t.id);
    }

    return result
      .sort((a, b) => byPriority(a.text) - byPriority(b.text))
      .slice(0, SUGGESTION_LIMIT);
  }
}