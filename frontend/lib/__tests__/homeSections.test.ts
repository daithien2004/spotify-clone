import { describe, it, expect } from "vitest";
import { buildHomeSections } from "@/lib/homeSections";
import type { PlaylistSummaryItem } from "@/services/api/playlistService";
import type { TrackResponse } from "@/lib/adapters";

describe("buildHomeSections", () => {
  it("returns an empty sections list when no data", () => {
    const sections = buildHomeSections([], []);
    expect(sections).toEqual([]);
  });

  it("maps playlists to a 'Your playlists' section", () => {
    const playlists: PlaylistSummaryItem[] = [
      {
        id: "pl-1",
        title: "Chill Mix",
        owner: "davedirect3",
        coverUrl: "/figma/chill-mix.png",
      },
    ];
    const sections = buildHomeSections(playlists, []);

    expect(sections).toHaveLength(1);
    expect(sections[0]?.title).toBe("Your playlists");
    expect(sections[0]?.items).toEqual([
      {
        id: "pl-1",
        title: "Chill Mix",
        description: "davedirect3",
        imageUrl: "/figma/chill-mix.png",
        type: "Playlist",
      },
    ]);
  });

  it("falls back to DEFAULT_COVER when playlist cover is missing", () => {
    const playlists: PlaylistSummaryItem[] = [
      { id: "pl-2", title: "Pop Mix", owner: "davedirect3", coverUrl: "" },
    ];
    const sections = buildHomeSections(playlists, []);

    expect(sections[0]?.items[0]?.imageUrl).toBe("/figma/chill-mix.png");
  });

  it("maps tracks to a 'Trending tracks' section with artist as description", () => {
    const tracks: TrackResponse[] = [
      {
        id: "tr-1",
        title: "Free Spirit",
        artist: "Khalid",
        album: "Free Spirit (Explicit)",
        durationMs: 182000,
        artworkUrl: null,
        audioUrl: null,
        createdAt: "2026-03-03T00:00:00Z",
        updatedAt: "2026-03-03T00:00:00Z",
      },
    ];
    const sections = buildHomeSections([], tracks);

    expect(sections).toHaveLength(1);
    expect(sections[0]?.title).toBe("Trending tracks");
    expect(sections[0]?.items).toEqual([
      {
        id: "tr-1",
        title: "Free Spirit",
        description: "Khalid",
        imageUrl: "/figma/chill-mix.png",
        type: "Track",
      },
    ]);
  });

  it("uses the track artwork when present", () => {
    const tracks: TrackResponse[] = [
      {
        id: "tr-2",
        title: "Play It Safe",
        artist: "Julia Wolf",
        album: "Girls In Purgatory",
        durationMs: 159000,
        artworkUrl: "/figma/happy-hits.png",
        audioUrl: "/api/v1/tracks/tr-2/audio",
        createdAt: "2026-03-03T00:00:00Z",
        updatedAt: "2026-03-03T00:00:00Z",
      },
    ];
    const sections = buildHomeSections([], tracks);

    expect(sections[0]?.items[0]?.imageUrl).toBe("/figma/happy-hits.png");
  });

  it("returns both sections in order when both present", () => {
    const sections = buildHomeSections(
      [{ id: "pl-1", title: "Chill Mix", owner: "davedirect3", coverUrl: "" }],
      [
        {
          id: "tr-1",
          title: "Free Spirit",
          artist: "Khalid",
          album: "Free Spirit (Explicit)",
          durationMs: 182000,
          artworkUrl: null,
          audioUrl: null,
          createdAt: "",
          updatedAt: "",
        },
      ]
    );

    expect(sections.map((s) => s.title)).toEqual(["Your playlists", "Trending tracks"]);
  });
});