import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { PlaylistService } from "@/services/api/playlistService";
import { queryKeys } from "@/lib/queryKeys";

/**
 * Thêm 1 track vào 1 playlist (backend LexoRank append). Invalidate tracks của
 * playlist đó để bảng track phản ánh membership mới khi mở lại.
 */
export function useAddTrackToPlaylist() {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: ({ playlistId, trackId }: { playlistId: string; trackId: string }) =>
      PlaylistService.addTrack(playlistId, trackId),
    onSuccess: () => {
      toast.success("Added to playlist");
    },
    onError: (err: Error) => {
      toast.error("Could not add track", { description: err.message });
    },
    onSettled: (_data, _err, vars) => {
      qc.invalidateQueries({ queryKey: queryKeys.playlists.tracks(vars.playlistId) });
    },
  });
}
