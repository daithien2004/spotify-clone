"use client";

import React from "react";
import { usePlayerStore } from "@/hooks/usePlayerStore";
import { TrackInfo } from "./player/TrackInfo";
import { PlaybackControls } from "./player/PlaybackControls";
import { PlayerProgress } from "./player/PlayerProgress";
import { VolumeControl } from "./player/VolumeControl";

export function Player() {
  const { isPlaying, togglePlay, currentTrack, volume, progress, setVolume } = usePlayerStore();

  return (
    <div className="h-24 glass-dark px-4 flex items-center justify-between transition-colors">
      <TrackInfo currentTrack={currentTrack} />

      <div className="flex flex-col items-center gap-2 max-w-[40%] w-full">
        <PlaybackControls 
          isPlaying={isPlaying} 
          onTogglePlay={togglePlay} 
        />
        <PlayerProgress 
          progress={progress} 
          currentTime="1:23" 
          totalTime="3:45" 
        />
      </div>

      <VolumeControl 
        volume={volume} 
        onVolumeChange={setVolume} 
      />
    </div>
  );
}
