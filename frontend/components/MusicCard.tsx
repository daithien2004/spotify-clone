"use client";

import React, { memo, useCallback } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { Play } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { usePlayerStore } from '@/hooks/usePlayerStore';

interface MusicCardProps {
  title: string;
  description: string;
  imageUrl: string;
  type?: string;
  className?: string;
  /** "artist" → bo tròn giống avatar nghệ sĩ (Nghệ sĩ phổ biến) */
  variant?: "track" | "artist";
  /** Có href → toàn bộ card là Link điều hướng (playlist) thay vì play track. */
  href?: string;
}

export const MusicCard = memo(function MusicCard({
  id,
  title,
  description,
  imageUrl,
  type,
  variant = "track",
  className,
  priority = false,
  href,
}: MusicCardProps & { id: string; priority?: boolean }) {
  const [hasError, setHasError] = React.useState(false);

  const setCurrentTrack = usePlayerStore((state) => state.setCurrentTrack);
  const setIsPlaying = usePlayerStore((state) => state.setIsPlaying);

  const isArtist = variant === "artist";
  const ariaLabel = `Phát ${title}${type ? ` (${type})` : ''}`;
  const fallbackUrl =
    'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=300&auto=format&fit=crop';

  const handlePlay = useCallback((e?: React.MouseEvent | React.KeyboardEvent) => {
    e?.stopPropagation();
    setCurrentTrack({
      id,
      title,
      artist: description.split(',')[0] || 'Artist',
      imageUrl,
      duration: 225,
    });
    setIsPlaying(true);
  }, [id, title, description, imageUrl, setCurrentTrack, setIsPlaying]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handlePlay(e);
    }
  }, [handlePlay]);

  // Overlay play chuyển thành span trang trí khi là Link — button trong anchor là invalid HTML.
  const playOverlay = href ? (
    <span aria-hidden className="pointer-events-none absolute bottom-2 right-2 opacity-0 transition-all duration-300 translate-y-2 group-hover:translate-y-0 group-hover:opacity-100">
      <span className="flex h-12 w-12 items-center justify-center rounded-full bg-accent-primary text-accent-primary-foreground shadow-xl">
        <Play className="h-6 w-6 fill-current" />
      </span>
    </span>
  ) : (
    <div className="absolute bottom-2 right-2 opacity-0 group-hover:opacity-100 transition-all duration-300 transform translate-y-2 group-hover:translate-y-0">
      <Button
        size="icon"
        onClick={(e) => {
          e.stopPropagation();
          handlePlay();
        }}
        title={ariaLabel}
        aria-label={ariaLabel}
        tabIndex={-1} // Handled by Card
        className="h-12 w-12 rounded-full bg-accent-primary text-accent-primary-foreground hover:bg-accent-primary/90 shadow-xl hover:scale-105 transition-transform active:scale-95"
      >
        <Play className="h-6 w-6 fill-current" />
      </Button>
    </div>
  );

  const cardClass = `group relative bg-bg-secondary/40 hover:bg-bg-tertiary/80 border-none transition-all duration-300 transform hover:-translate-y-1 overflow-hidden shadow-lg p-3 cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-accent-primary/50 ${className}`;

  const inner = (
    <div className="flex h-full flex-col gap-4 p-3">
      {/* Ảnh bìa — bo tròn khi là nghệ sĩ (giống avatar khoanh tròn ở Figma) */}
      <div
        className={`relative aspect-square overflow-hidden shadow-2xl bg-bg-secondary ${
          isArtist ? "rounded-full mx-auto max-w-[200px]" : "rounded-[var(--radius-image-card)]"
        }`}
      >
        <Image
          src={hasError ? fallbackUrl : imageUrl}
          alt={`Ảnh bìa cho ${title}`}
          fill
          sizes="(max-width: 768px) 50vw, (max-width: 1200px) 25vw, 20vw"
          priority={priority}
          onError={() => setHasError(true)}
          className="object-cover transition-transform duration-500 group-hover:scale-110"
        />
        {playOverlay}
      </div>
      <div className="space-y-1">
        <h3 className="font-bold text-foreground truncate text-base antialiased">
          {title}
        </h3>
        <p className="text-sm text-muted-foreground line-clamp-2 leading-relaxed">
          {description}
        </p>
      </div>
    </div>
  );

  if (href) {
    return (
      <Link
        href={href}
        aria-label={`Xem ${title}`}
        className={`block ${cardClass} hover:-translate-y-1`}
      >
        {inner}
      </Link>
    );
  }

  return (
    <div
      onClick={() => handlePlay()}
      onKeyDown={handleKeyDown}
      role="button"
      tabIndex={0}
      className={cardClass}
    >
      {inner}
    </div>
  );
});