"use client";

import React from 'react';
import { Plus, ArrowRight, Library } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { SidebarItem, LIBRARY_ITEMS } from '@/lib/mockData';

const LibraryItem = React.memo(({ item }: { item: SidebarItem }) => (
  <div
    className="group flex items-center gap-3 rounded-lg p-2 transition-all hover:bg-muted cursor-pointer"
    aria-label={`Mở ${item.name}`}
  >
    <Avatar
      className={`h-12 w-12 transition-transform group-active:scale-95 ${item.type === 'Artist' ? 'rounded-full' : 'rounded-md'}`}
    >
      <AvatarImage
        src={item.imageUrl}
        alt={item.name}
        className="object-cover"
      />
      <AvatarFallback>{item.name[0]}</AvatarFallback>
    </Avatar>
    <div className="flex flex-col">
      <span className="font-medium text-foreground group-hover:text-primary transition-colors uppercase tracking-tight antialiased">
        {item.name}
      </span>
      <span className="text-sm text-muted-foreground">
        {item.subtitle || item.type}
      </span>
    </div>
  </div>
));

LibraryItem.displayName = 'LibraryItem';

function LibraryHeader() {
  return (
    <div className="flex items-center justify-between px-4 py-3 shadow-md border-b border-border/50">
      <div className="flex items-center gap-3 text-muted-foreground hover:text-foreground transition-colors cursor-pointer group">
        <Library className="h-6 w-6 group-hover:scale-110 transition-transform" />
        <span className="font-bold">Thư viện</span>
      </div>
      <div className="flex items-center gap-2">
        <Button
          variant="ghost"
          size="icon"
          aria-label="Tạo danh sách phát hoặc thư mục mới"
          className="h-8 w-8 text-muted-foreground hover:text-foreground hover:bg-muted rounded-full"
        >
          <Plus className="h-5 w-5" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          aria-label="Xem thêm"
          className="h-8 w-8 text-muted-foreground hover:text-foreground hover:bg-muted rounded-full"
        >
          <ArrowRight className="h-5 w-5" />
        </Button>
      </div>
    </div>
  );
}

export function LibrarySidebar() {
  return (
    <div className="flex h-full flex-col bg-card rounded-xl overflow-hidden shadow-2xl transition-colors">
      <LibraryHeader />

      <ScrollArea className="flex-1 px-4 pt-2">
        <div className="space-y-4 pb-4">
          {LIBRARY_ITEMS.map((item: SidebarItem) => (
            <LibraryItem key={item.id} item={item} />
          ))}
        </div>
      </ScrollArea>
    </div>
  );
}
