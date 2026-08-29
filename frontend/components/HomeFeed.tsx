"use client";

import { useQuery } from "@tanstack/react-query";
import { ScrollArea } from "@/components/ui/scroll-area";
import { HOME_SECTIONS } from "@/lib/musicData";
import { queryKeys } from "@/lib/queryKeys";
import { SectionHeader } from "./SectionHeader";
import { MusicCard } from "./MusicCard";

function greeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
}

export default function HomeFeed() {
  // Data đã được prefetch trên server (app/(main)/page.tsx).
  const { data: sections } = useQuery({
    queryKey: queryKeys.home.sections(),
    queryFn: async () => HOME_SECTIONS,
    initialData: HOME_SECTIONS, // Fallback an toàn nếu hydration trễ.
  });

  return (
    <ScrollArea className="@container h-full overflow-hidden rounded-[4px] bg-bg-secondary shadow-2xl ring-1 ring-inset ring-border-feed">
      <div className="space-y-8 p-6">
        <h1 className="text-[2.75rem] font-bold leading-tight text-text-primary">
          {greeting()}
        </h1>

        {sections?.map((section) => (
          <section key={section.title} className="space-y-4">
            <SectionHeader
              title={section.title}
              showAllLabel="SEE ALL"
              onShowAll={() => console.log(`Show all: ${section.title}`)}
            />
            <div className="grid grid-cols-2 gap-4 @lg:grid-cols-3 @2xl:grid-cols-4">
              {section.items.map((item, index) => (
                <MusicCard
                  key={item.id}
                  id={item.id}
                  title={item.title}
                  description={item.description}
                  imageUrl={item.imageUrl}
                  priority={index < 2}
                  variant={item.variant}
                />
              ))}
            </div>
          </section>
        ))}
      </div>
    </ScrollArea>
  );
}