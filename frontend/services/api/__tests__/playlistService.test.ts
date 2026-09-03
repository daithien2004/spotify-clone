import { describe, it, expect, vi, beforeEach } from "vitest";
import { api, type ApiResponse } from "@/lib/api-client";
import { PlaylistService } from "@/services/api/playlistService";

vi.mock("@/lib/api-client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api-client")>();
  return {
    ...actual,
    api: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    },
    unwrap: (env: ApiResponse) => env.data,
  };
});

const PLAYLIST_RESPONSE = {
  id: "p1",
  title: "My Mix",
  description: "hand-picked",
  ownerName: "Alice",
  coverUrl: null,
  createdAt: "2026-09-03T00:00:00Z",
  updatedAt: "2026-09-03T00:00:00Z",
};

describe("PlaylistService", () => {
  beforeEach(() => vi.clearAllMocks());

  it("createPlaylist POST /playlists và unwrap data -> Playlist domain", async () => {
    vi.mocked(api.post).mockResolvedValue({
      success: true,
      data: PLAYLIST_RESPONSE,
      timestamp: "",
    } as ApiResponse<typeof PLAYLIST_RESPONSE>);

    const playlist = await PlaylistService.createPlaylist({ title: "My Mix", description: "hand-picked" });

    expect(api.post).toHaveBeenCalledWith("/playlists", {
      title: "My Mix",
      description: "hand-picked",
    });
    expect(playlist.id).toBe("p1");
    expect(playlist.title).toBe("My Mix");
    expect(playlist.owner).toBe("Alice");
  });

  it("addTrack POST /playlists/{id}/tracks với body { trackId }", async () => {
    vi.mocked(api.post).mockResolvedValue({ success: true, timestamp: "" } as ApiResponse<unknown>);

    await PlaylistService.addTrack("p1", "t1");

    expect(api.post).toHaveBeenCalledWith("/playlists/p1/tracks", { trackId: "t1" });
  });
});
