"use client";

import Image from "next/image";
import { useParams } from "next/navigation";
import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Heart, MoreHorizontal, Play } from "lucide-react";
import { TrackTable } from "@/components/playlist/TrackTable";
import { Button } from "@/components/ui/button";
import { queryKeys } from "@/lib/queryKeys";
import {
  formatDurationLabel,
  toPlayerTrack,
  trackResponseToTrackItem,
} from "@/lib/adapters";
import { PlaylistService } from "@/services/api/playlistService";
import { TrackService } from "@/services/api/trackService";
import { usePlayerStore } from "@/hooks/usePlayerStore";

/** Màn Playlist (Figma Chill Mix 131:2938): header gradient + TrackTable.
 *  Dữ liệu thật từ playlist-service + track-service (join theo trackId, lexoRank). */
export default function PlaylistPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;

  const playQueue = usePlayerStore((s) => s.playQueue);

  const playlistQuery = useQuery({
    queryKey: queryKeys.playlists.detail(id),
    queryFn: () => PlaylistService.getPlaylist(id),
    retry: false,
  });

  const tracksQuery = useQuery({
    queryKey: queryKeys.playlists.tracks(id),
    queryFn: () => PlaylistService.getPlaylistTracks(id),
    retry: false,
  });

  const trackIds = useMemo(
    () => tracksQuery.data?.map((m) => m.trackId) ?? [],
    [tracksQuery.data]
  );

  const metadataQuery = useQuery({
    queryKey: queryKeys.tracks.list({ ids: trackIds }),
    queryFn: () => TrackService.getTracksByIds(trackIds),
    enabled: trackIds.length > 0,
  });

  // Join membership (thứ tự lexoRank) với metadata → TrackItem list.
  const songs = useMemo(() => {
    const tracks = metadataQuery.data;
    if (!tracks || !tracksQuery.data) return [];
    const byId = new Map(tracks.map((t) => [t.id, t]));
    return tracksQuery.data.flatMap((membership) => {
      const t = byId.get(membership.trackId);
      return t ? [trackResponseToTrackItem(t)] : [];
    });
  }, [tracksQuery.data, metadataQuery.data]);

  const playlist = playlistQuery.data;
  const loading = playlistQuery.isPending || tracksQuery.isPending;
  const notFound = playlistQuery.isError || tracksQuery.isError;

  if (notFound) {
    return (
      <div className="flex h-full items-center justify-center p-8">
        <p className="text-sm text-text-muted">Playlist not found.</p>
      </div>
    );
  }
  if (loading || !playlist) {
    return (
      <div className="flex h-full items-center justify-center p-8">
        <p className="text-sm text-text-muted">Loading playlist…</p>
      </div>
    );
  }

  const totalSec = songs.reduce((sum, s) => sum + s.durationSec, 0);
  const artistNames = Array.from(new Set(songs.map((s) => s.artist))).join(", ");

  const handlePlay = () => {
    if (songs.length === 0) return;
    playQueue(songs.map(toPlayerTrack), 0);
  };

  return (
    <div className="h-full overflow-y-auto">
      <header
        className={`flex items-end gap-6 bg-gradient-to-b p-6 pb-4 ${playlist.gradient}`}
      >
        <div className="relative h-40 w-40 shrink-0 overflow-hidden rounded shadow-2xl lg:h-52 lg:w-52">
          <Image
            src={playlist.coverUrl}
            alt={playlist.title}
            fill
            sizes="(max-width: 767px) 160px, 208px"
            priority
            className="object-cover"
          />
        </div>
        <div className="min-w-0">
          <p className="text-xs font-bold uppercase tracking-widest text-text-primary/90">
            {playlist.type}
          </p>
          <h1 className="mt-1.5 text-4xl font-bold leading-tight text-text-primary lg:text-5xl">
            {playlist.title}
          </h1>
          <p className="mt-3 truncate text-sm text-text-soft">
            {artistNames ? `${artistNames} and more` : playlist.description}
          </p>
          <p className="mt-2 text-sm text-text-soft">
            Made for <span className="font-bold text-text-primary">You</span>
          </p>
          <p className="mt-2 text-xs font-medium text-text-soft">
            {playlist.owner} · {playlist.type} · {songs.length} songs,{" "}
            {formatDurationLabel(totalSec) || "—"}
          </p>
        </div>
      </header>

      <div className="flex items-center gap-6 bg-bg-secondary/80 px-6 py-4">
        <Button
          onClick={handlePlay}
          className="h-12 w-12 rounded-full bg-accent-primary p-0 text-accent-primary-foreground shadow-xl hover:scale-105 hover:bg-accent-primary/90 transition-transform"
          aria-label={`Play ${playlist.title}`}
        >
          <Play className="h-6 w-6 fill-current" />
        </Button>
        <button
          type="button"
          aria-label="Save to Liked Songs"
          className="text-text-soft transition-colors hover:text-accent-primary"
        >
          <Heart className="h-7 w-7" />
        </button>
        <button
          type="button"
          aria-label="More options"
          className="text-text-soft transition-colors hover:text-text-primary"
        >
          <MoreHorizontal className="h-7 w-7" />
        </button>
      </div>

      {/* Track table — gradient dần về nền chính */}
      <div className="bg-gradient-to-b from-bg-secondary to-bg-primary pb-4 pt-2">
        <TrackTable
          tracks={songs}
          onPlayRow={(t, index) => playQueue(songs.map(toPlayerTrack), index)}
        />
      </div>
    </div>
  );
}