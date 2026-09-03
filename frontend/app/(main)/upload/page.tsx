"use client";

import { useState, useRef } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useTrackUpload } from "@/hooks/useTrackUpload";
import { Loader2, Upload } from "lucide-react";

/**
 * Upload track: người dùng chọn file audio + nhập metadata → FE đọc duration
 * từ file (readAudioDurationMs) → POST /tracks (tạo metadata) → PUT /tracks/{id}/audio.
 * Route nằm trong (main) layout nên Player vẫn persist.
 */
export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState("");
  const [artist, setArtist] = useState("");
  const [album, setAlbum] = useState("");
  const [durationLabel, setDurationLabel] = useState<string>("");
  const inputRef = useRef<HTMLInputElement>(null);
  const uploadMutation = useTrackUpload();

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0] ?? null;
    setFile(selected);
    if (selected) {
      // Hiển thị tên file và chuẩn bị cho submit (duration sẽ đọc lúc mutation chạy)
      setDurationLabel(selected.name);
    } else {
      setDurationLabel("");
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!file || !title.trim() || !artist.trim()) return;
    uploadMutation.mutate({
      title: title.trim(),
      artist: artist.trim(),
      album: album.trim() || undefined,
      file,
    });
  };

  const canSubmit = file && title.trim() && artist.trim() && !uploadMutation.isPending;

  return (
    <div className="mx-auto w-full max-w-[500px] px-4 py-12 animate-in fade-in slide-in-from-bottom-4 duration-1000 transition-colors">
      <div className="mb-8 flex flex-col items-center text-center">
        <div className="mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-bg-tertiary">
          <Upload className="h-10 w-10 text-text-muted" />
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-text-primary">Upload a track</h1>
        <p className="mt-1 text-sm text-text-muted">
          Add a new track to your catalog. Supported: MP3, WAV, FLAC, OGG.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div className="space-y-2">
          <Label htmlFor="audio-file" className="text-sm font-bold text-text-primary">
            Audio file
          </Label>
          <Input
            ref={inputRef}
            id="audio-file"
            type="file"
            accept="audio/*"
            onChange={handleFileChange}
            className="h-12 cursor-pointer bg-background text-foreground file:mr-4 file:rounded-full file:border-0 file:bg-bg-tertiary file:px-4 file:py-2 file:text-sm file:font-semibold file:text-text-primary hover:file:bg-white/10"
          />
          {durationLabel && (
            <p className="truncate text-xs text-text-muted">{durationLabel}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="title" className="text-sm font-bold text-text-primary">
            Title
          </Label>
          <Input
            id="title"
            type="text"
            placeholder="My awesome track"
            required
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="h-12 bg-background border-border hover:border-foreground focus:border-foreground focus:ring-1 focus:ring-ring text-foreground placeholder:text-muted-foreground transition-all rounded-[4px]"
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="artist" className="text-sm font-bold text-text-primary">
            Artist
          </Label>
          <Input
            id="artist"
            type="text"
            placeholder="Artist name"
            required
            value={artist}
            onChange={(e) => setArtist(e.target.value)}
            className="h-12 bg-background border-border hover:border-foreground focus:border-foreground focus:ring-1 focus:ring-ring text-foreground placeholder:text-muted-foreground transition-all rounded-[4px]"
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="album" className="text-sm font-bold text-text-primary">
            Album <span className="text-text-muted">(optional)</span>
          </Label>
          <Input
            id="album"
            type="text"
            placeholder="Album name"
            value={album}
            onChange={(e) => setAlbum(e.target.value)}
            className="h-12 bg-background border-border hover:border-foreground focus:border-foreground focus:ring-1 focus:ring-ring text-foreground placeholder:text-muted-foreground transition-all rounded-[4px]"
          />
        </div>

        <Button
          className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full transition-transform active:scale-[0.98] disabled:opacity-70"
          type="submit"
          disabled={!canSubmit}
        >
          {uploadMutation.isPending ? (
            <div className="flex items-center gap-2">
              <Loader2 className="w-5 h-5 animate-spin" />
              <span>Uploading…</span>
            </div>
          ) : (
            "Upload"
          )}
        </Button>
      </form>
    </div>
  );
}
