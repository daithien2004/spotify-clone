import React from "react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export interface ControlButtonProps {
  icon: React.ReactNode;
  onClick?: () => void;
  ariaLabel: string;
}

/** Button icon chuẩn cho player — dùng chung PlaybackControls/VolumeControl (trước đây bị duplicate). */
export function ControlButton({ icon, onClick, ariaLabel }: ControlButtonProps) {
  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={onClick}
      aria-label={ariaLabel}
      title={ariaLabel}
      className="text-text-hint hover:text-text-primary transition-colors"
    >
      {React.isValidElement(icon)
        ? React.cloneElement(icon as React.ReactElement<{ className?: string }>, {
            className: cn("size-4", (icon.props as { className?: string }).className),
          })
        : icon}
    </Button>
  );
}