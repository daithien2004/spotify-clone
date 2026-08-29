import { Volume2, Mic2, LayoutList, MonitorSpeaker, Maximize2 } from "lucide-react";
import { Slider } from "@/components/ui/slider";
import { ControlButton } from "./ControlButton";

interface VolumeControlProps {
  volume: number;
  onVolumeChange: (value: number) => void;
}

export function VolumeControl({ volume, onVolumeChange }: VolumeControlProps) {
  return (
    <div className="flex items-center justify-end gap-3 w-1/3">
      <ControlButton icon={<Mic2 />} ariaLabel="Lyrics" />
      <ControlButton icon={<LayoutList />} ariaLabel="Queue" />
      <ControlButton icon={<MonitorSpeaker />} ariaLabel="Connect to a device" />
      <div className="flex items-center gap-2 w-32 group">
        <Volume2 className="size-4 text-text-hint group-hover:text-text-primary transition-colors" />
        <Slider
          value={[volume * 100]}
          onValueChange={(vals) => onVolumeChange(vals[0] / 100)}
          max={100}
          step={1}
          className="w-full cursor-pointer"
          aria-label="Volume"
        />
      </div>
      <ControlButton icon={<Maximize2 />} ariaLabel="Fullscreen" />
    </div>
  );
}