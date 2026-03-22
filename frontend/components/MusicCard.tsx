"use client";

import React, { memo, useCallback } from 'react';
import Image from 'next/image';
import { Play } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { usePlayerStore } from '@/hooks/usePlayerStore';

interface MusicCardProps {
  title: string;
  description: string;
  imageUrl: string;
  type?: string;
  className?: string;
}

export const MusicCard = memo(function MusicCard({
  id,
  title,
  description,
  imageUrl,
  type,
  className,
  priority = false,
}: MusicCardProps & { id: string; priority?: boolean }) {
  const [hasError, setHasError] = React.useState(false);
  
  // Logic: Use individual selectors and wrap handlers in useCallback for peak stability
  const setCurrentTrack = usePlayerStore((state) => state.setCurrentTrack);
  const setIsPlaying = usePlayerStore((state) => state.setIsPlaying);

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

  return (
    <Card
      onClick={() => handlePlay()}
      onKeyDown={handleKeyDown}
      role="button"
      tabIndex={0}
      className={`group relative bg-card/40 hover:bg-muted/80 border-none transition-all duration-300 transform hover:-translate-y-1 overflow-hidden shadow-lg p-3 cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-green-500 ${className}`}
    >
      <CardContent className="p-0 space-y-4">
        <div className="relative aspect-square overflow-hidden rounded-md shadow-2xl bg-muted">
          <Image
            src={hasError ? fallbackUrl : imageUrl}
            alt={`Ảnh bìa cho ${title}`}
            fill
            sizes="(max-width: 768px) 50vw, (max-width: 1200px) 25vw, 20vw"
            priority={priority}
            onError={() => setHasError(true)}
            className="object-cover transition-transform duration-500 group-hover:scale-110"
          />
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
              className="h-12 w-12 rounded-full bg-green-500 text-black hover:bg-green-400 shadow-xl hover:scale-105 transition-transform active:scale-95"
            >
              <Play className="h-6 w-6 fill-current" />
            </Button>
          </div>
        </div>
        <div className="space-y-1">
          <h3 className="font-bold text-foreground truncate text-base antialiased">
            {title}
          </h3>
          <p className="text-sm text-muted-foreground line-clamp-2 leading-relaxed">
            {description}
          </p>
        </div>
      </CardContent>
    </Card>
  );
});
