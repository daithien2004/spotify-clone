import { describe, it, expect } from "vitest";
import { resolveApiUrl } from "@/lib/api-client";

/**
 * Regression-guard cho resolveApiUrl — bug P4: audioUrl từ backend đã kèm prefix
 * `/api/v1/tracks/...`, resolveApiUrl gắn thêm BASE_URL (`http://localhost:9000/api/v1`)
 * tạo URL kép `/api/v1/api/v1/...` → 401/format error khi Player phát stream.
 */
describe("resolveApiUrl", () => {
  it("path đã kèm /api/v1 prefix → không gắn base kép (fix double-path)", () => {
    const url = resolveApiUrl("/api/v1/tracks/20000000-0000-4000-8000-000000000001/audio");
    expect(url).toBe(
      "http://localhost:9000/api/v1/tracks/20000000-0000-4000-8000-000000000001/audio"
    );
  });

  it("path tương đối (vd /tracks/..) → gắn đúng base gateway", () => {
    expect(resolveApiUrl("/tracks/abc/audio")).toBe("http://localhost:9000/api/v1/tracks/abc/audio");
  });

  it("path không có / đầu → tự thêm / trước BASE_URL", () => {
    expect(resolveApiUrl("tracks/abc/audio")).toBe("http://localhost:9000/api/v1/tracks/abc/audio");
  });

  it("URL tuyệt đối (http) → trả nguyên, không đổi", () => {
    expect(resolveApiUrl("https://cdn.example.com/x.mp3")).toBe("https://cdn.example.com/x.mp3");
  });
});
