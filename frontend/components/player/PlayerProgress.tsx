import React from "react";
import { Slider } from "@/components/ui/slider";

interface PlayerProgressProps {
  progress: number;
  onProgressChange?: (value: number) => void;
  currentTime?: string;
  totalTime?: string;
}

export function PlayerProgress({ 
  progress, 
  onProgressChange, 
  currentTime = "0:00", 
  totalTime = "0:00" 
}: PlayerProgressProps) {
  return (
    <div className="flex items-center gap-2 w-full max-w-lg">
      <span className="text-xs text-muted-foreground tabular-nums">{currentTime}</span>
      <Slider 
        value={[progress]} 
        onValueChange={(vals) => onProgressChange?.(vals[0])}
        max={100} 
        step={1} 
        className="w-full h-1 cursor-pointer hover:accent-primary" 
        aria-label="Thời lượng bài hát" 
      />
      <span className="text-xs text-muted-foreground tabular-nums">{totalTime}</span>
    </div>
  );
}
