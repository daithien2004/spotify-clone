"use client";

import { memo, useCallback } from "react";
import { Search as SearchIcon } from "lucide-react";
import type { SearchSuggestion } from "@/services/search/searchService";

interface SearchSuggestionRowProps {
  suggestion: SearchSuggestion;
  onPick: (suggestion: SearchSuggestion) => void;
}

/** Row gợi ý tìm kiếm (Figma 13:2) — click điền vào ô search. */
export const SearchSuggestionRow = memo(function SearchSuggestionRow({
  suggestion,
  onPick,
}: SearchSuggestionRowProps) {
  const handlePick = useCallback(() => {
    onPick(suggestion);
  }, [suggestion, onPick]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        handlePick();
      }
    },
    [handlePick]
  );

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={handlePick}
      onKeyDown={handleKeyDown}
      aria-label={`Tìm kiếm ${suggestion.text}`}
      className="group flex w-full cursor-pointer items-center gap-3 rounded-md px-3 py-2 text-left transition-colors hover:bg-bg-tertiary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent-primary/60"
    >
      <span className="flex size-[27px] shrink-0 items-center justify-center rounded-md bg-bg-elevated text-text-muted">
        <SearchIcon className="h-4 w-4" aria-hidden />
      </span>
      <p className="truncate text-sm text-text-primary">{suggestion.text}</p>
    </div>
  );
});