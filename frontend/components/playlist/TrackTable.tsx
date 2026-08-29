"use client";

import type { TrackItem } from "@/lib/musicTypes";
import { TrackRow } from "./TrackRow";

/** Track table (Figma 131:2938). */
export function TrackTable({ tracks }: { tracks: TrackItem[] }) {
  return (
    <div role="table" aria-label="Tracks" className="w-full">
      {/* Header */}
      <div
        role="row"
        className="grid grid-cols-[32px_minmax(0,1fr)_minmax(0,1fr)_minmax(0,0.9fr)_48px] items-center gap-4 border-b border-border-feed/60 px-4 py-3 text-xs font-medium uppercase tracking-wider text-text-muted"
      >
        <span className="text-right">#</span>
        <span>TITLE</span>
        <span>ALBUM</span>
        <span>DATE ADDED</span>
        <span className="flex justify-end">⏱</span>
      </div>

      {/* Custom order nhắc (Figma có dòng "Custom order") */}
      <div className="flex items-center gap-2 px-4 py-2 text-xs text-text-muted">
        <span className="inline-block h-1.5 w-1.5 rounded-full bg-bg-tertiary" />
        Custom order
      </div>

      <div className="space-y-0.5 py-2">
        {tracks.map((track, index) => (
          <TrackRow key={track.id} index={index} track={track} />
        ))}
      </div>
    </div>
  );
}