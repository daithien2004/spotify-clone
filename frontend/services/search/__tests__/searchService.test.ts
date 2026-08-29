import { describe, it, expect } from "vitest";
import { SearchService } from "@/services/search/searchService";
import type { TrackItem } from "@/lib/musicTypes";

const INDEX: TrackItem[] = [
  {
    id: "t1",
    title: "Free Spirit",
    artist: "Khalid",
    album: "Free Spirit (Explicit)",
    durationSec: 182,
    dateAdded: "Mar 2, 2026",
  },
  {
    id: "t2",
    title: "Ocean Front Apt.",
    artist: "ayokay",
    album: "Digital Dreamscape",
    durationSec: 132,
    dateAdded: "Mar 1, 2026",
    coverUrl: "/figma/chill-mix.png",
  },
  {
    id: "t3",
    title: "Sunday Best",
    artist: "Surfaces",
    album: "Where the Light Is",
    durationSec: 158,
    dateAdded: "Mar 1, 2026",
  },
];

describe("SearchService", () => {
  describe("searchTracks", () => {
    it("returns [] when query is empty or whitespace-only", () => {
      expect(SearchService.searchTracks("", INDEX)).toEqual([]);
      expect(SearchService.searchTracks("   ", INDEX)).toEqual([]);
    });

    it("matches title case-insensitively and trims query", () => {
      const results = SearchService.searchTracks("  free spirit ", INDEX);
      expect(results.map((t) => t.id)).toEqual(["t1"]);
    });

    it("matches artist name", () => {
      const results = SearchService.searchTracks("ayokay", INDEX);
      expect(results.map((t) => t.id)).toEqual(["t2"]);
    });

    it("matches album name", () => {
      const results = SearchService.searchTracks("where the light is", INDEX);
      expect(results.map((t) => t.id)).toEqual(["t3"]);
    });

    it("returns [] when nothing matches", () => {
      expect(SearchService.searchTracks("zzzz", INDEX)).toEqual([]);
    });

    it("returns full SearchResult shape with coverUrl", () => {
      const results = SearchService.searchTracks("ocean", INDEX);
      expect(results[0]).toEqual({
        id: "t2",
        title: "Ocean Front Apt.",
        artist: "ayokay",
        album: "Digital Dreamscape",
        coverUrl: "/figma/chill-mix.png",
      });
    });
  });

  describe("searchSuggestions", () => {
    it("returns [] when query is empty", () => {
      expect(SearchService.searchSuggestions("", INDEX)).toEqual([]);
    });

    it("suggests matching titles and artists, deduped", () => {
      const suggestions = SearchService.searchSuggestions("free", INDEX);
      expect(suggestions.map((s) => s.text)).toEqual(["Free Spirit"]);
    });

    it("suggests from artist when title does not match", () => {
      const suggestions = SearchService.searchSuggestions("khalid", INDEX);
      expect(suggestions.map((s) => s.text)).toEqual(["Khalid"]);
    });

    it("caps suggestions at 5 entries", () => {
      const bigIndex: TrackItem[] = Array.from({ length: 10 }, (_, i) => ({
        id: `b${i}`,
        title: `Match Song ${i}`,
        artist: `Match Artist ${i}`,
        album: "Album",
        durationSec: 100,
        dateAdded: "Mar 1, 2026",
      }));
      const suggestions = SearchService.searchSuggestions("match", bigIndex);
      expect(suggestions.length).toBeLessThanOrEqual(5);
    });

    it("returns stable ids for dedup across identical texts", () => {
      const suggestions = SearchService.searchSuggestions("free", INDEX);
      const ids = new Set(suggestions.map((s) => s.id));
      expect(ids.size).toBe(suggestions.length);
    });
  });
});