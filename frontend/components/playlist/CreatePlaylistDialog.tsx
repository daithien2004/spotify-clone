"use client";

import { useState } from "react";
import { Loader2, Plus } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCreatePlaylist } from "@/hooks/useCreatePlaylist";

/**
 * Dialog tạo playlist mới từ sidebar ("Create Playlist"). Backend POST /playlists
 * lấy owner từ X-User-Id; sau khi tạo → redirect tới /playlist/{id} (hook tự làm).
 *
 * @param withLabel khi true, trigger là nút pill "＋ Create Playlist" (sidebar Figma);
 *        khi false, chỉ icon Plus vuông.
 */
export function CreatePlaylistDialog({ withLabel = false }: { withLabel?: boolean }) {
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const createMutation = useCreatePlaylist();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || createMutation.isPending) return;
    createMutation.mutate(
      { title: title.trim() },
      { onSuccess: () => setOpen(false) }
    );
  };

  const canSubmit = title.trim().length > 0 && !createMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {withLabel ? (
          <Button
            type="button"
            variant="ghost"
            className="flex items-center gap-1 rounded-full px-3 py-2 text-sm font-bold text-text-soft transition-colors hover:bg-white/10 hover:text-text-primary"
          >
            <Plus className="mr-1 h-4 w-4" />
            Create Playlist
          </Button>
        ) : (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label="Create playlist"
            className="h-9 w-9 rounded-full text-text-soft hover:bg-white/10 hover:text-text-primary"
          >
            <Plus className="h-5 w-5" />
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Create playlist</DialogTitle>
          <DialogDescription>
            Give your playlist a name. You can add tracks to it from any track&apos;s
            context menu.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="playlist-title" className="text-sm font-semibold text-text-primary">
              Name
            </Label>
            <Input
              id="playlist-title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="My Playlist"
              autoFocus
              required
              className="h-11 bg-background border-border hover:border-foreground focus:border-foreground focus:ring-1 focus:ring-ring text-foreground placeholder:text-muted-foreground transition-all rounded-[4px]"
            />
          </div>
          <DialogFooter>
            <Button
              type="submit"
              disabled={!canSubmit}
              className="bg-spotify-green hover:opacity-90 text-black font-bold rounded-full"
            >
              {createMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                "Create"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
