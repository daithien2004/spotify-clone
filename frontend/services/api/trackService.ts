/**
 * Track API (track-service) — batch metadata GET. Streaming endpoint
 * (/tracks/{id}/audio) được dùng trực tiếp làm <audio src> qua resolveApiUrl.
 */
import { api, type ApiResponse, unwrap } from "@/lib/api-client";
import type { TrackResponse } from "@/lib/adapters";

/** Metadata tạo track mới — durationMs do FE đọc từ file (readAudioDurationMs). */
export interface CreateTrackInput {
  title: string;
  artist: string;
  album?: string;
  durationMs: number;
}

export class TrackService {
  /** GET /tracks?ids=a,b,c — giữ thứ tự như id đầu vào. */
  static async getTracksByIds(ids: string[]): Promise<TrackResponse[]> {
    if (ids.length === 0) return [];
    const envelope = await api.get<ApiResponse<TrackResponse[]>>(
      `/tracks?ids=${ids.join(",")}`
    );
    return unwrap(envelope);
  }

  /** GET /tracks — list tất cả tracks (Home feed "Trending tracks", seed 6). */
  static async listTracks(): Promise<TrackResponse[]> {
    const envelope = await api.get<ApiResponse<TrackResponse[]>>("/tracks");
    return unwrap(envelope);
  }

  /** POST /tracks — tạo metadata track, trả về track mới kèm id (chưa có audio). */
  static async createTrack(input: CreateTrackInput): Promise<TrackResponse> {
    const envelope = await api.post<ApiResponse<TrackResponse>>("/tracks", input);
    return unwrap(envelope);
  }

  /** PUT /tracks/{id}/audio — upload file audio gắn vào track đã tạo. */
  static async uploadAudio(trackId: string, file: File): Promise<void> {
    const formData = new FormData();
    formData.append("file", file);
    // Content-Type undefined → axios tự set multipart kèm boundary; giữ default
    // application/json sẽ làm backend không nhận file.
    await api.put(`/tracks/${trackId}/audio`, formData, {
      headers: { "Content-Type": undefined },
    });
  }
}