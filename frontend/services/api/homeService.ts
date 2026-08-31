/**
 * Home feed API — gộp playlist + tracks thành sections.
 * HomeFeed gọi service này; không nhìn thấy chi tiết 2 API riêng.
 */
import { PlaylistService } from "@/services/api/playlistService";
import { TrackService } from "@/services/api/trackService";
import { buildHomeSections, type HomeSection } from "@/lib/homeSections";

export class HomeService {
  /** Sections thật cho Home feed: playlists + trending tracks. */
  static async getHomeSections(): Promise<HomeSection[]> {
    const [playlists, tracks] = await Promise.all([
      PlaylistService.listPlaylists(),
      TrackService.listTracks(),
    ]);
    return buildHomeSections(playlists, tracks);
  }
}