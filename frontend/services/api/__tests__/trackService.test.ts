import { describe, it, expect, vi, beforeEach } from "vitest";
import { api, type ApiResponse } from "@/lib/api-client";
import { TrackService } from "@/services/api/trackService";

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

const TRACK = {
  id: "t1",
  title: "Play It Safe",
  artist: "Julia Wolf",
  album: "Girls In Purgatory",
  durationMs: 159000,
  artworkUrl: null,
  audioUrl: null,
  createdAt: "2026-03-03T00:00:00Z",
  updatedAt: "2026-03-03T00:00:00Z",
};

describe("TrackService", () => {
  beforeEach(() => vi.clearAllMocks());

  it("createTrack posts metadata tới /tracks và unwrap data", async () => {
    vi.mocked(api.post).mockResolvedValue({
      success: true,
      data: TRACK,
      timestamp: "",
    } as ApiResponse<typeof TRACK>);

    const input = { title: "T", artist: "A", album: "Alb", durationMs: 1000 };

    await expect(TrackService.createTrack(input)).resolves.toEqual(TRACK);
    expect(api.post).toHaveBeenCalledWith("/tracks", input);
  });

  it("uploadAudio gửi FormData chứa file, header cho axios tự set multipart boundary", async () => {
    vi.mocked(api.put).mockResolvedValue(undefined);
    const file = new File(["data"], "song.mp3", { type: "audio/mpeg" });

    await TrackService.uploadAudio("t1", file);

    const [url, body, config] = vi.mocked(api.put).mock.calls[0];
    expect(url).toBe("/tracks/t1/audio");
    expect(body).toBeInstanceOf(FormData);
    expect((body as FormData).get("file")).toBe(file);
    // Để Content-Type = undefined → axios tự set multipart/form-data kèm boundary
    // (default instance là application/json, giữ sẽ làm backend không nhận file).
    expect(config?.headers?.["Content-Type"]).toBeUndefined();
  });
});
