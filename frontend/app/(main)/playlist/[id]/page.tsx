import { notFound } from "next/navigation";
import Image from "next/image";
import { Heart, MoreHorizontal, Play } from "lucide-react";
import { PLAYLISTS } from "@/lib/musicData";
import { TrackTable } from "@/components/playlist/TrackTable";
import { Button } from "@/components/ui/button";

/** Màn Playlist (Figma Chill Mix 131:2938): header gradient + TrackTable. */
export default async function PlaylistPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const playlist = PLAYLISTS[id];

  if (!playlist) {
    notFound();
  }

  const artistNames = Array.from(
    new Set(playlist.songs.map((s) => s.artist))
  ).join(", ");

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
            {artistNames} and more
          </p>
          <p className="mt-2 text-sm text-text-soft">
            Made for <span className="font-bold text-text-primary">You</span>
          </p>
          <p className="mt-2 text-xs font-medium text-text-soft">
            {playlist.owner} · {playlist.type} · {playlist.totalSongs} songs,{" "}
            {playlist.totalDurationLabel}
          </p>
        </div>
      </header>

      <div className="flex items-center gap-6 bg-bg-secondary/80 px-6 py-4">
        <Button
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
        <TrackTable tracks={playlist.songs} />
      </div>
    </div>
  );
}