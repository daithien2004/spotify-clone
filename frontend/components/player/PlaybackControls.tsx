import React from "react";
import { 
  Play, 
  SkipBack, 
  SkipForward, 
  Repeat, 
  Shuffle 
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

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
  onSkipForward 
}: PlaybackControlsProps) {
  return (
    <div className="flex items-center gap-6">
      <ControlButton icon={<Shuffle />} ariaLabel="Trộn bài" />
      <ControlButton 
        icon={<SkipBack className="fill-current" />} 
        ariaLabel="Bài trước" 
        onClick={onSkipBack}
      />
      <Button 
        size="icon" 
        aria-label={isPlaying ? "Tạm dừng" : "Phát"}
        onClick={onTogglePlay}
        className="h-10 w-10 rounded-full bg-foreground text-background hover:scale-110 active:scale-95 transition-all shadow-xl"
      >
        {isPlaying ? (
          <div className="flex gap-1 items-center justify-center">
            <div className="w-1.5 h-4 bg-background rounded-full" />
            <div className="w-1.5 h-4 bg-background rounded-full" />
          </div>
        ) : (
          <Play className="h-6 w-6 fill-current ml-1" />
        )}
      </Button>
      <ControlButton 
        icon={<SkipForward className="fill-current" />} 
        ariaLabel="Bài tiếp theo" 
        onClick={onSkipForward}
      />
      <ControlButton icon={<Repeat />} ariaLabel="Lặp lại" />
    </div>
  );
}

interface ControlButtonProps {
  icon: React.ReactNode;
  onClick?: () => void;
  ariaLabel: string;
}

function ControlButton({ icon, onClick, ariaLabel }: ControlButtonProps) {
  return (
    <Button 
      variant="ghost" 
      size="icon" 
      onClick={onClick}
      aria-label={ariaLabel}
      title={ariaLabel}
      className="text-muted-foreground hover:text-foreground transition-colors group"
    >
      {React.isValidElement(icon) 
        ? React.cloneElement(icon as React.ReactElement<{ className?: string }>, { 
            className: cn("h-5 w-5", (icon.props as { className?: string }).className) 
          }) 
        : icon}
    </Button>
  );
}
