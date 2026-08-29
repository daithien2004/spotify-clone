"use client";

import { memo, useCallback, useState } from "react";
import Image from "next/image";
import { Music2, Play } from "lucide-react";
import { cn } from "@/lib/utils";
import type { SearchResult } from "@/services/search/searchService";

const FALLBACK_COVER = "/figma/chill-mix.png";

interface SearchResultRowProps {
  result: SearchResult;
  onSelect: (result: SearchResult) => void;
}

/** Row kết quả bài hát (Figma 13:2): cover 60×60, Play hiện khi hover. */
export const SearchResultRow = memo(function SearchResultRow({
  result,
  onSelect,
}: SearchResultRowProps) {
  const [hasError, setHasError] = useState(false);

  const handleSelect = useCallback(() => {
    onSelect(result);
  }, [result, onSelect]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        handleSelect();
      }
    },
    [handleSelect]
  );

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={handleSelect}
      onKeyDown={handleKeyDown}
      aria-label={`Phát bài hát ${result.title} — ${result.artist}`}
      className="group flex w-full cursor-pointer items-center gap-3 rounded-md px-3 py-2 text-left transition-colors hover:bg-bg-tertiary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent-primary/60"
    >
      <div className="relative h-15 w-15 shrink-0 overflow-hidden rounded-[4px] bg-bg-elevated shadow-md">
        {result.coverUrl ? (
          <Image
            src={hasError ? FALLBACK_COVER : result.coverUrl}
            alt=""
            fill
            sizes="60px"
            aria-hidden
            onError={() => setHasError(true)}
            className="object-cover"
          />
        ) : (
          <span className="flex h-full w-full items-center justify-center text-text-muted">
            <Music2 className="h-5 w-5" aria-hidden />
          </span>
        )}
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-bold text-text-primary">{result.title}</p>
        <p className="truncate text-xs text-text-muted">Bài hát • {result.artist}</p>
      </div>

      <Play
        className={cn(
          "h-5 w-5 shrink-0 fill-none text-text-muted transition-opacity",
          "opacity-0 group-hover:opacity-100 group-focus-visible:opacity-100"
        )}
        aria-hidden
      />
    </div>
  );
});