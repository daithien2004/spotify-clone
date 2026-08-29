import React, { memo } from "react";

interface SectionHeaderProps {
  title: string;
  subtitle?: string;
  onShowAll?: () => void;
  className?: string;
  /** Nhãn nút "xem tất cả" — Figma Home dùng "SEE ALL". */
  showAllLabel?: string;
}

export const SectionHeader = memo(function SectionHeader({
  title,
  subtitle,
  onShowAll,
  className = "mb-4",
  showAllLabel = "Hiện tất cả",
}: SectionHeaderProps) {
  return (
    <div className={`flex items-center justify-between ${className}`}>
      <div>
        {subtitle ? (
          <p className="text-sm font-bold text-muted-foreground">
            {subtitle}
          </p>
        ) : null}
        <h2 className="text-3xl font-bold text-foreground tracking-tight hover:underline cursor-pointer transition-all">
          {title}
        </h2>
      </div>
      <button
        onClick={onShowAll}
        aria-label={`Xem tất cả ${title}`}
        className="text-sm font-bold text-muted-foreground hover:underline cursor-pointer transition-colors"
      >
        {showAllLabel}
      </button>
    </div>
  );
});
