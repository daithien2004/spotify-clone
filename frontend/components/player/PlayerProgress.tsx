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
  totalTime = "0:00",
}: PlayerProgressProps) {
  return (
    <div className="flex items-center gap-2 w-full max-w-lg">
      <span className="text-xs text-text-hint tabular-nums">{currentTime}</span>
      <Slider
        value={[progress]}
        onValueChange={(vals) => onProgressChange?.(vals[0])}
        max={100}
        step={1}
        className="w-full cursor-pointer"
        aria-label="Song duration"
      />
      <span className="text-xs text-text-hint tabular-nums">{totalTime}</span>
    </div>
  );
}