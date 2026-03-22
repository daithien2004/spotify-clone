import React from "react";
import Image from "next/image";
import { Track } from "@/hooks/usePlayerStore";

interface TrackInfoProps {
  currentTrack: Track | null;
}

export function TrackInfo({ currentTrack }: TrackInfoProps) {
  return (
    <div className="flex items-center gap-4 min-w-[180px] w-1/3">
      <div className="h-14 w-14 bg-muted rounded-md overflow-hidden shadow-lg relative">
        {currentTrack?.imageUrl ? (
          <Image 
            src={currentTrack.imageUrl} 
            alt={currentTrack.title} 
            fill 
            sizes="56px"
            className="object-cover"
          />
        ) : (
          <div className="h-full w-full bg-muted animate-pulse" />
        )}
      </div>
      <div className="flex flex-col">
        <span className="text-sm font-bold text-foreground hover:underline cursor-pointer line-clamp-1">
          {currentTrack?.title || "Song Title"}
        </span>
        <span className="text-xs text-muted-foreground hover:text-foreground hover:underline cursor-pointer transition-colors line-clamp-1">
          {currentTrack?.artist || "Artist Name"}
        </span>
      </div>
    </div>
  );
}
