import { Play, Pause, SkipBack, SkipForward, Repeat, Shuffle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ControlButton } from "./ControlButton";

interface PlaybackControlsProps {
  isPlaying: boolean;
  onTogglePlay: () => void;
  onSkipBack?: () => void;
  onSkipForward?: () => void;
}

export function PlaybackControls({
  isPlaying,
  onTogglePlay,
  onSkipBack,
  onSkipForward,
}: PlaybackControlsProps) {
  return (
    <div className="flex items-center gap-6">
      <ControlButton icon={<Shuffle />} ariaLabel="Shuffle" />
      <ControlButton
        icon={<SkipBack className="fill-current" />}
        ariaLabel="Previous song"
        onClick={onSkipBack}
      />
      <Button
        size="icon"
        aria-label={isPlaying ? "Pause" : "Play"}
        onClick={onTogglePlay}
        className="h-10 w-10 rounded-full bg-surface-primary text-text-on-white hover:scale-105 active:scale-95 transition-all shadow-xl"
      >
        {isPlaying ? (
          <Pause className="size-6 fill-current" />
        ) : (
          <Play className="size-6 fill-current" />
        )}
      </Button>
      <ControlButton
        icon={<SkipForward className="fill-current" />}
        ariaLabel="Next song"
        onClick={onSkipForward}
      />
      <ControlButton icon={<Repeat />} ariaLabel="Repeat" />
    </div>
  );
}