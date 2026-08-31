"use client";

import { useEffect, useRef, useState } from "react";
import { usePlayerStore } from "@/hooks/usePlayerStore";
import { resolveApiUrl } from "@/lib/api-client";
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
  const audioRef = useRef<HTMLAudioElement>(null);
  // Độ dài thật từ metadata audio; fallback về duration của track.
  const [realDuration, setRealDuration] = useState<number | null>(null);

  const isPlaying = usePlayerStore((s) => s.isPlaying);
  const togglePlay = usePlayerStore((s) => s.togglePlay);
  const currentTrack = usePlayerStore((s) => s.currentTrack);
  const progress = usePlayerStore((s) => s.progress);
  const setProgress = usePlayerStore((s) => s.setProgress);
  const volume = usePlayerStore((s) => s.volume);
  const setVolume = usePlayerStore((s) => s.setVolume);
  const next = usePlayerStore((s) => s.next);
  const previous = usePlayerStore((s) => s.previous);

  const duration = realDuration ?? currentTrack?.duration ?? 0;
  const currentTime = formatTime((progress / 100) * duration);
  const totalTime = formatTime(duration);

  const audioUrl = currentTrack?.audioUrl
    ? resolveApiUrl(currentTrack.audioUrl)
    : null;

  // Đổi src khi chuyển track (audio hidden; data-src để tránh set lại vô ích).
  // Không reset realDuration ngay đây (violate react-hooks/set-state-in-effect):
  // browser fire onLoadStart khi src đổi → reset tại event handler.
  useEffect(() => {
    const el = audioRef.current;
    if (!el) return;
    if (audioUrl && el.dataset.src !== currentTrack?.audioUrl) {
      el.dataset.src = currentTrack?.audioUrl;
      el.src = audioUrl;
    }
  }, [audioUrl, currentTrack?.audioUrl]);

  // Play/pause đồng bộ với store.
  useEffect(() => {
    const el = audioRef.current;
    if (!el || !audioUrl) return;
    if (isPlaying) {
      el.play().catch(() => usePlayerStore.getState().setIsPlaying(false));
    } else {
      el.pause();
    }
  }, [isPlaying, audioUrl]);

  // Volume 0..1 direct map.
  useEffect(() => {
    const el = audioRef.current;
    if (el) el.volume = volume;
  }, [volume]);

  const handleSeek = (value: number) => {
    setProgress(value);
    const el = audioRef.current;
    if (el && duration > 0) {
      el.currentTime = (value / 100) * duration;
    }
  };

  return (
    <div className="h-[88px] shrink-0 bg-bg-primary border-t border-white/5 px-4 flex items-center justify-between">
      <audio
        ref={audioRef}
        className="hidden"
        preload="metadata"
        onLoadStart={() => setRealDuration(null)}
        onTimeUpdate={() => {
          const el = audioRef.current;
          if (el && Number.isFinite(el.duration) && el.duration > 0) {
            setProgress((el.currentTime / el.duration) * 100);
          }
        }}
        onLoadedMetadata={() => {
          const el = audioRef.current;
          if (el && Number.isFinite(el.duration)) setRealDuration(el.duration);
        }}
        onEnded={next}
        onError={() => usePlayerStore.getState().setIsPlaying(false)}
      />

      <TrackInfo currentTrack={currentTrack} />

      {/* flex-1 min-w-0: cho phép cột giữa co lại khi vào màn hình hẹp,
          thay cho w-full trước đó gây tràn khi cộng với 2 cột w-1/3. */}
      <div className="flex flex-col items-center gap-2 flex-1 min-w-0 max-w-[40%]">
        <PlaybackControls
          isPlaying={isPlaying}
          onTogglePlay={togglePlay}
          onSkipBack={previous}
          onSkipForward={next}
        />
        <PlayerProgress
          progress={progress}
          onProgressChange={handleSeek}
          currentTime={currentTime}
          totalTime={totalTime}
        />
      </div>

      <VolumeControl volume={volume} onVolumeChange={setVolume} />
    </div>
  );
}