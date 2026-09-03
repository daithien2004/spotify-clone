import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { PlaylistService } from "@/services/api/playlistService";
import { queryKeys } from "@/lib/queryKeys";

/**
 * Tạo playlist mới từ sidebar. Backend POST /playlists lấy owner từ X-User-Id;
 * hook invalidate list để sidebar refresh rồi chuyển tới trang playlist vừa tạo.
 */
export function useCreatePlaylist() {
  const qc = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: (input: { title: string; description?: string }) =>
      PlaylistService.createPlaylist(input),
    onSuccess: async (playlist) => {
      await qc.invalidateQueries({ queryKey: queryKeys.playlists.list() });
      toast.success("Playlist created");
      router.push(`/playlist/${playlist.id}`);
    },
    onError: (err: Error) => {
      toast.error("Could not create playlist", { description: err.message });
    },
  });
}
