/**
 * Track API (track-service) — batch metadata GET. Streaming endpoint
 * (/tracks/{id}/audio) được dùng trực tiếp làm <audio src> qua resolveApiUrl.
 */
import { api, type ApiResponse, unwrap } from "@/lib/api-client";
import type { TrackResponse } from "@/lib/adapters";

export class TrackService {
  /** GET /tracks?ids=a,b,c — giữ thứ tự như id đầu vào. */
  static async getTracksByIds(ids: string[]): Promise<TrackResponse[]> {
    if (ids.length === 0) return [];
    const envelope = await api.get<ApiResponse<TrackResponse[]>>(
      `/tracks?ids=${ids.join(",")}`
    );
    return unwrap(envelope);
  }
}