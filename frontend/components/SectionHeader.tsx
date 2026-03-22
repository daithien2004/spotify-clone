import React, { memo } from "react";

interface SectionHeaderProps {
  title: string;
  subtitle?: string;
  onShowAll?: () => void;
  className?: string;
}

export const SectionHeader = memo(function SectionHeader({ 
  title, 
  subtitle, 
  onShowAll, 
  className = "mb-4" 
}: SectionHeaderProps) {
  return (
    <div className={`flex items-center justify-between ${className}`}>
      <div>
        {subtitle ? (
          <p className="text-sm font-bold text-muted-foreground">
            {subtitle}
          </p>
        ) : null}
        <h2 className="text-2xl font-bold text-foreground tracking-tight hover:underline cursor-pointer transition-all">
          {title}
        </h2>
      </div>
      <button 
        onClick={onShowAll}
        aria-label={`Xem tất cả ${title}`}
        className="text-sm font-bold text-muted-foreground hover:underline cursor-pointer transition-colors"
      >
        Hiện tất cả
      </button>
    </div>
  );
});
