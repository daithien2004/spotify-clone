import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { TrackService, type CreateTrackInput } from "@/services/api/trackService";
import { readAudioDurationMs } from "@/lib/audio";
import { queryKeys } from "@/lib/queryKeys";

/**
 * Input cho upload — durationMs lấy từ file, không cần truyền từ user.
 * backend POST /tracks bắt buộc durationMs (thêm durationMs từ readAudioDurationMs).
 */
interface UploadInput extends Omit<CreateTrackInput, "durationMs"> {
  file: File;
}

export function useTrackUpload() {
  const qc = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: async (input: UploadInput) => {
      const durationMs = await readAudioDurationMs(input.file);
      const track = await TrackService.createTrack({
        title: input.title,
        artist: input.artist,
        album: input.album,
        durationMs,
      });
      await TrackService.uploadAudio(track.id, input.file);
      return track;
    },
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: queryKeys.tracks.all });
      toast.success("Track uploaded successfully");
      router.push("/");
    },
    onError: (err: Error) => {
      toast.error("Upload failed", { description: err.message });
    },
  });
}
