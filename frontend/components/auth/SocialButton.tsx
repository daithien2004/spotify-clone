import React from "react";
import { Button } from "@/components/ui/button";

interface SocialButtonProps {
  icon: React.ReactNode;
  text: string;
  onClick?: () => void;
  className?: string;
}

export function SocialButton({ icon, text, onClick, className }: SocialButtonProps) {
  return (
    <Button
      variant="outline"
      onClick={onClick}
      className={`w-full h-12 bg-background border-border hover:border-foreground text-foreground font-bold rounded-full flex items-center justify-center gap-3 transition-all relative group ${className}`}
    >
      <div className="absolute left-4">
        {icon}
      </div>
      <span className="flex-1 text-center">{text}</span>
    </Button>
  );
}
