import { describe, it, expect, beforeEach } from "vitest";
import { usePlayerStore } from "@/hooks/usePlayerStore";

describe("usePlayerStore", () => {
  beforeEach(() => {
    // Reset về trạng thái mặc định trước mỗi test (tránh persist/localStorage lẫn).
    localStorage.clear();
    usePlayerStore.setState({
      isPlaying: false,
      currentTrack: null,
      volume: 0.7,
      progress: 0,
    });
  });

  it("togglePlay flips isPlaying", () => {
    usePlayerStore.getState().togglePlay();
    expect(usePlayerStore.getState().isPlaying).toBe(true);
    usePlayerStore.getState().togglePlay();
    expect(usePlayerStore.getState().isPlaying).toBe(false);
  });

  it("setCurrentTrack stores the track", () => {
    const track = {
      id: "t1",
      title: "Play It Safe",
      artist: "Julia Wolf",
      imageUrl: "/figma/chill-mix.png",
      duration: 225,
    };
    usePlayerStore.getState().setCurrentTrack(track);
    expect(usePlayerStore.getState().currentTrack).toEqual(track);
  });

  it("setVolume stores float 0..1 without clamping (caller responsibility)", () => {
    usePlayerStore.getState().setVolume(0.3);
    expect(usePlayerStore.getState().volume).toBe(0.3);
  });

  it("partialize only persists volume + currentTrack", () => {
    usePlayerStore.getState().setVolume(0.5);
    usePlayerStore.getState().setProgress(42);
    const persisted = localStorage.getItem("spotify-player-storage");
    expect(persisted).not.toBeNull();
    const parsed = JSON.parse(persisted!) as { state?: { isPlaying?: unknown; progress?: unknown; volume?: unknown } };
    expect(parsed.state?.volume).toBe(0.5);
    expect(parsed.state?.progress).toBeUndefined();
    expect(parsed.state?.isPlaying).toBeUndefined();
  });
});