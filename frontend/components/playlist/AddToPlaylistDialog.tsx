"use client";

import { useQuery } from "@tanstack/react-query";
import { FolderPlus } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAddTrackToPlaylist } from "@/hooks/useAddTrackToPlaylist";
import { queryKeys } from "@/lib/queryKeys";
import { PlaylistService } from "@/services/api/playlistService";
import type { TrackItem } from "@/lib/musicTypes";

/**
 * Dialog "Add to playlist" — liệt kê playlist của user (sidebar API), chọn một
 * cái → POST /playlists/{id}/tracks rồi đóng. Backend LexoRank append vào cuối.
 */
export function AddToPlaylistDialog({
  track,
  open,
  onOpenChange,
}: {
  track: TrackItem;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const playlistsQuery = useQuery({
    queryKey: queryKeys.playlists.list(),
    queryFn: PlaylistService.listPlaylists,
    staleTime: 30_000,
  });
  const addMutation = useAddTrackToPlaylist();

  const playlists = playlistsQuery.data ?? [];

  const handleAdd = (playlistId: string) => {
    addMutation.mutate(
      { playlistId, trackId: track.id },
      { onSuccess: () => onOpenChange(false) }
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Add to playlist</DialogTitle>
          <DialogDescription>
            Choose a playlist to add “{track.title}”.
          </DialogDescription>
        </DialogHeader>
        <div className="flex max-h-72 flex-col gap-1 overflow-y-auto">
          {playlists.length === 0 ? (
            <p className="px-1 py-3 text-sm text-text-muted">
              No playlists yet. Create one from the sidebar first.
            </p>
          ) : (
            playlists.map((pl) => (
              <button
                key={pl.id}
                type="button"
                disabled={addMutation.isPending}
                onClick={() => handleAdd(pl.id)}
                className="flex items-center gap-3 rounded-md px-3 py-2 text-left text-sm font-medium text-text-primary transition-colors hover:bg-white/10 disabled:opacity-60"
              >
                <FolderPlus className="h-4 w-4 shrink-0 text-text-muted" />
                {pl.title}
              </button>
            ))
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
