import React from "react";
import { 
  Volume2, 
  Mic2, 
  LayoutList, 
  MonitorSpeaker, 
  Maximize2 
} from "lucide-react";
import { Slider } from "@/components/ui/slider";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface VolumeControlProps {
  volume: number;
  onVolumeChange: (value: number) => void;
}

export function VolumeControl({ volume, onVolumeChange }: VolumeControlProps) {
  return (
    <div className="flex items-center justify-end gap-3 w-1/3">
      <ControlButton icon={<Mic2 />} ariaLabel="Lời bài hát" />
      <ControlButton icon={<LayoutList />} ariaLabel="Danh sách chờ" />
      <ControlButton icon={<MonitorSpeaker />} ariaLabel="Kết nối với thiết bị" />
      <div className="flex items-center gap-2 w-32 group">
        <Volume2 className="h-5 w-5 text-muted-foreground group-hover:text-foreground transition-colors" />
        <Slider 
          value={[volume * 100]} 
          onValueChange={(vals) => onVolumeChange(vals[0] / 100)}
          max={100} 
          step={1} 
          className="w-full h-1" 
          aria-label="Âm lượng" 
        />
      </div>
      <ControlButton icon={<Maximize2 />} ariaLabel="Toàn màn hình" />
    </div>
  );
}

function ControlButton({ icon, onClick, ariaLabel }: { icon: React.ReactNode, onClick?: () => void, ariaLabel: string }) {
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
