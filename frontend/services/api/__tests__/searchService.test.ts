import { describe, it, expect, vi, beforeEach } from "vitest";

// Mock the api-client module entirely — no axios network in unit tests.
vi.mock("@/lib/api-client", () => ({
  api: { get: vi.fn() },
  unwrap: (envelope: { data: unknown }) => envelope.data,
}));

import { api } from "@/lib/api-client";
import { SearchApiService } from "@/services/api/searchService";

describe("SearchApiService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("returns [] without calling the API when query is blank", async () => {
    expect(await SearchApiService.search("   ")).toEqual([]);
    expect(api.get).not.toHaveBeenCalled();
  });

  it("calls /search/tracks with q and limit params, then unwraps data", async () => {
    const envelope = {
      success: true,
      data: [{ id: "1", title: "Free Spirit", artist: "Khalid", album: "Free Spirit (Explicit)" }],
      message: "ok",
      timestamp: "2026-08-29T10:00:00Z",
    };
    vi.mocked(api.get).mockResolvedValue(envelope);

    const result = await SearchApiService.search("khalid", 5);

    expect(api.get).toHaveBeenCalledWith("/search/tracks", { params: { q: "khalid", limit: 5 } });
    expect(result).toEqual(envelope.data);
  });

  it("passes the default limit 10 when none given", async () => {
    vi.mocked(api.get).mockResolvedValue({
      success: true,
      data: [],
      message: "ok",
      timestamp: "2026-08-29T10:00:00Z",
    });

    await SearchApiService.search("ocean");

    expect(api.get).toHaveBeenCalledWith("/search/tracks", { params: { q: "ocean", limit: 10 } });
  });
});