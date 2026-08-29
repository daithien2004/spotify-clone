"use client";

import { usePlayerStore } from "@/hooks/usePlayerStore";
import { TrackInfo } from "./player/TrackInfo";
import { PlaybackControls } from "./player/PlaybackControls";
import { PlayerProgress } from "./player/PlayerProgress";
import { VolumeControl } from "./player/VolumeControl";

/** Logic: format số giây → "m:ss"; null/NaN/âm → "0:00". */
function formatTime(seconds: number | null): string {
  if (seconds === null || !Number.isFinite(seconds) || seconds < 0) return "0:00";
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, "0")}`;
}

export function Player() {
  const isPlaying = usePlayerStore((s) => s.isPlaying);
  const togglePlay = usePlayerStore((s) => s.togglePlay);
  const currentTrack = usePlayerStore((s) => s.currentTrack);
  const progress = usePlayerStore((s) => s.progress);
  const setProgress = usePlayerStore((s) => s.setProgress);
  const volume = usePlayerStore((s) => s.volume);
  const setVolume = usePlayerStore((s) => s.setVolume);

  // Thời gian thật theo track đang phát; progress là %, nên quy về giây.
  const duration = currentTrack?.duration ?? 0;
  const currentTime = formatTime((progress / 100) * duration);
  const totalTime = formatTime(duration);

  return (
    <div className="h-[88px] shrink-0 bg-bg-primary border-t border-white/5 px-4 flex items-center justify-between">
      <TrackInfo currentTrack={currentTrack} />

      {/* flex-1 min-w-0: cho phép cột giữa co lại khi vào màn hình hẹp,
          thay cho w-full trước đó gây tràn khi cộng với 2 cột w-1/3. */}
      <div className="flex flex-col items-center gap-2 flex-1 min-w-0 max-w-[40%]">
        <PlaybackControls isPlaying={isPlaying} onTogglePlay={togglePlay} />
        <PlayerProgress
          progress={progress}
          onProgressChange={setProgress}
          currentTime={currentTime}
          totalTime={totalTime}
        />
      </div>

      <VolumeControl volume={volume} onVolumeChange={setVolume} />
    </div>
  );
}