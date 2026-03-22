"use client";

import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { DAILY_MIXES } from "@/lib/mockData";
import { SectionHeader } from "./SectionHeader";
import { MusicCard } from "./MusicCard";

export function HomeFeed() {
  // Masterclass: Using prefetched data from the server
  const { data: mixes } = useQuery({
    queryKey: ["daily-mixes"],
    queryFn: async () => DAILY_MIXES, // In a real app, this is an API call
    initialData: DAILY_MIXES, // Fallback for safety
  });

  return (
    <ScrollArea className="h-full bg-gradient-to-b from-muted-foreground/10 to-background rounded-xl overflow-hidden shadow-2xl transition-colors">
      <div className="p-6 space-y-4">
        <div className="flex gap-2 sticky top-0 bg-transparent z-10 pt-2">
          <Badge className="bg-foreground text-background hover:bg-foreground/90 px-4 py-1.5 rounded-full cursor-pointer text-sm font-bold transition-all">Tất cả</Badge>
        </div>

        <section>
          <SectionHeader 
            title="_thienn10_" 
            subtitle="Dành Cho" 
            onShowAll={() => console.log("Show all daily mixes")} 
          />
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
            {mixes?.map((mix, index) => (
              <MusicCard 
                key={mix.id} 
                {...mix} 
                priority={index < 4}
              />
            ))}
          </div>
        </section>
      </div>
    </ScrollArea>
  );
}
