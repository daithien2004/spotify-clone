"use client";

import { useCallback } from "react";
import Image from "next/image";
import { Clock } from "lucide-react";
import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuSeparator,
  ContextMenuTrigger,
} from "@/components/ui/context-menu";
import { usePlayerStore } from "@/hooks/usePlayerStore";
import type { TrackItem } from "@/lib/musicTypes";

/** Dòng track trong bảng playlist (Figma 131:2938); right-click = context menu (325:7536). */

function formatDuration(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

export function TrackRow({
  index,
  track,
}: {
  index: number;
  track: TrackItem;
}) {
  const setCurrentTrack = usePlayerStore((state) => state.setCurrentTrack);
  const setIsPlaying = usePlayerStore((state) => state.setIsPlaying);

  const playTrack = useCallback(() => {
    setCurrentTrack({
      id: track.id,
      title: track.title,
      artist: track.artist,
      imageUrl: track.coverUrl ?? "/figma/daily-mix-4.png",
      duration: track.durationSec,
    });
    setIsPlaying(true);
  }, [track, setCurrentTrack, setIsPlaying]);

  return (
    <ContextMenu>
      <ContextMenuTrigger asChild>
        <div
          role="row"
          tabIndex={0}
          onClick={playTrack}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              playTrack();
            }
          }}
          className="group grid cursor-pointer grid-cols-[32px_minmax(0,1fr)_minmax(0,1fr)_minmax(0,0.9fr)_48px] items-center gap-4 rounded-md px-4 py-2 text-sm text-text-muted outline-none transition-colors hover:bg-white/10 focus-visible:ring-2 focus-visible:ring-accent-primary/40"
        >
          <span className="text-right tabular-nums text-text-muted group-hover:hidden">
            {index + 1}
          </span>
          <span className="hidden text-center tabular-nums text-accent-primary group-hover:block">
            ▶
          </span>

          <div className="flex min-w-0 items-center gap-3">
            {track.coverUrl ? (
              <Image
                src={track.coverUrl}
                alt=""
                width={40}
                height={40}
                className="h-10 w-10 shrink-0 rounded object-cover"
                aria-hidden
              />
            ) : null}
            <div className="min-w-0">
              <p className="truncate font-medium text-text-primary group-hover:text-text-primary">
                {track.title}
              </p>
            </div>
          </div>

          <span className="truncate">{track.artist}</span>
          <span className="truncate">{track.dateAdded}</span>
          <span className="flex items-center justify-end tabular-nums">
            <Clock className="mr-1 h-3.5 w-3.5 opacity-0 transition-opacity group-hover:opacity-100" />
            {formatDuration(track.durationSec)}
          </span>
        </div>
      </ContextMenuTrigger>
      <ContextMenuContent className="w-56 rounded-lg bg-bg-tertiary/95 p-1.5 shadow-2xl backdrop-blur-md">
        <ContextMenuItem className="cursor-pointer rounded-md px-3 py-2 text-sm text-text-primary focus:bg-white/10">
          Add to queue
        </ContextMenuItem>
        <ContextMenuItem className="cursor-pointer rounded-md px-3 py-2 text-sm text-text-primary focus:bg-white/10">
          Go to song radio
        </ContextMenuItem>
        <ContextMenuItem className="cursor-pointer rounded-md px-3 py-2 text-sm text-text-primary focus:bg-white/10">
          Go to artist
        </ContextMenuItem>
        <ContextMenuItem className="cursor-pointer rounded-md px-3 py-2 text-sm text-text-primary focus:bg-white/10">
          Go to album
        </ContextMenuItem>
        <ContextMenuSeparator className="my-1 bg-border/50" />
        <ContextMenuItem className="cursor-pointer rounded-md px-3 py-2 text-sm text-text-primary focus:bg-white/10">
          Report
        </ContextMenuItem>
        <ContextMenuItem className="cursor-pointer rounded-md px-3 py-2 text-sm text-text-primary focus:bg-white/10">
          Show credits
        </ContextMenuItem>
        <ContextMenuSeparator className="my-1 bg-border/50" />
        <ContextMenuItem className="cursor-pointer rounded-md px-3 py-2 text-sm text-text-primary focus:bg-white/10">
          Saved to your Liked Songs
        </ContextMenuItem>
        <ContextMenuItem className="cursor-pointer rounded-md px-3 py-2 text-sm text-text-primary focus:bg-white/10">
          Add to playlist
        </ContextMenuItem>
        <ContextMenuItem className="cursor-pointer rounded-md px-3 py-2 text-sm text-text-primary focus:bg-white/10">
          Share
        </ContextMenuItem>
      </ContextMenuContent>
    </ContextMenu>
  );
}