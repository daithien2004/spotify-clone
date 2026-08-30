"use client";

import { useCallback, useEffect, useId, useMemo, useRef, useState } from "react";
import { Search as SearchIcon, X } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Input } from "@/components/ui/input";
import { SearchResultRow } from "@/components/search/SearchResultRow";
import { SearchSuggestionRow } from "@/components/search/SearchSuggestionRow";
import { usePlayerStore } from "@/hooks/usePlayerStore";
import { queryKeys } from "@/lib/queryKeys";
import { SearchApiService, type SearchItem } from "@/services/api/searchService";

/**
 * Search trên header (Figma 13:2): combobox giữa TopNav + dropdown
 * suggestions/kết quả cùng chiều rộng ô search.
 */
export function SearchBar() {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [open, setOpen] = useState(false);
  const listId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const setCurrentTrack = usePlayerStore((s) => s.setCurrentTrack);
  const setIsPlaying = usePlayerStore((s) => s.setIsPlaying);

  // Debounce 300ms — avoid one request per keystroke while typing.
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(timer);
  }, [query]);

  const tracksQuery = useQuery({
    queryKey: queryKeys.search.tracks(debouncedQuery),
    queryFn: () => SearchApiService.search(debouncedQuery),
    enabled: debouncedQuery.trim() !== "",
  });

  // Backend results are authoritative; map them onto the shapes the pure logic + rows expect.
  const liveIndex = useMemo<SearchItem[]>(() => tracksQuery.data ?? [], [tracksQuery.data]);
  const results = useMemo(
    () =>
      liveIndex.map((t) => ({
        id: t.id,
        title: t.title,
        artist: t.artist,
        album: t.album,
        coverUrl: t.artworkUrl,
      })),
    [liveIndex]
  );
  const suggestions = useMemo(
    () =>
      liveIndex.flatMap((t) => [
        { id: `${t.id}-title`, text: t.title },
        { id: `${t.id}-artist`, text: t.artist },
      ]),
    [liveIndex]
  );
  const isEmpty = query.trim() === "";
  const showDropdown = open && !isEmpty;
  const hasResults = suggestions.length > 0 || results.length > 0;

  // Click ra ngoài dropdown → đóng (mousedown để bắt kịp trước blur của input).
  const handleOutsideClick = useCallback((e: MouseEvent) => {
    if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
      setOpen(false);
    }
  }, []);

  useEffect(() => {
    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, [handleOutsideClick]);

  const handleQueryChange = useCallback((value: string) => {
    setQuery(value);
    setOpen(true);
  }, []);

  const handlePickSuggestion = useCallback((suggestion: { text: string }) => {
    setQuery(suggestion.text);
    setOpen(true); // Gợi ý điền query = nhập liệu tiếp — giữ dropdown mở.
  }, []);

  // Chọn bài hát → phát trên Player (giống MusicCard) và đóng dropdown.
  const handleSelectTrack = useCallback(
    (result: { id: string; title: string; artist: string; coverUrl?: string }) => {
      const track = liveIndex.find((t) => t.id === result.id);
      setCurrentTrack({
        id: result.id,
        title: result.title,
        artist: result.artist,
        imageUrl: result.coverUrl ?? track?.artworkUrl ?? "",
        duration: track?.durationMs ? Math.round(track.durationMs / 1000) : 0,
        audioUrl: track?.audioUrl,
      });
      setIsPlaying(true);
      setOpen(false);
    },
    [liveIndex, setCurrentTrack, setIsPlaying]
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === "Escape") {
        setOpen(false);
      }
    },
    []
  );

  return (
    <div ref={rootRef} className="relative w-[320px] max-w-[420px] flex-1">
      <SearchIcon className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-text-muted" />
      <Input
        role="combobox"
        aria-expanded={showDropdown}
        aria-controls={listId}
        aria-autocomplete="list"
        value={query}
        onChange={(e) => handleQueryChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Artists, songs, or albums"
        aria-label="Tìm kiếm bài hát, nghệ sĩ"
        className="h-12 w-full rounded-full border-none bg-bg-elevated pl-12 pr-12 text-text-primary placeholder:text-text-muted focus-visible:ring-2 focus-visible:ring-accent-primary/30"
      />
      {!isEmpty ? (
        <button
          type="button"
          onClick={() => handleQueryChange("")}
          aria-label="Xóa tìm kiếm"
          className="absolute right-3 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-full text-text-muted hover:text-text-primary"
        >
          <X className="h-4 w-4" />
        </button>
      ) : null}

      {showDropdown ? (
        <div
          id={listId}
          role="region"
          aria-label="Kết quả tìm kiếm"
          className="absolute left-0 right-0 top-full z-50 mt-2 max-h-[70vh] overflow-y-auto rounded-xl bg-bg-tertiary/95 p-1.5 shadow-2xl ring-1 ring-inset ring-border-feed backdrop-blur-md"
        >
          {hasResults ? (
            <>
              {suggestions.length > 0 ? (
                <ul className="py-1" role="presentation">
                  {suggestions.map((suggestion) => (
                    <SearchSuggestionRow
                      key={suggestion.id}
                      suggestion={suggestion}
                      onPick={handlePickSuggestion}
                    />
                  ))}
                </ul>
              ) : null}

              {results.length > 0 ? (
                <ul className="border-t border-border py-1" role="presentation">
                  {results.map((result) => (
                    <SearchResultRow
                      key={result.id}
                      result={result}
                      onSelect={handleSelectTrack}
                    />
                  ))}
                </ul>
              ) : null}
            </>
          ) : (
            <p className="py-8 text-center text-sm text-text-muted">
              No results for &quot;{query}&quot;
            </p>
          )}
        </div>
      ) : null}
    </div>
  );
}