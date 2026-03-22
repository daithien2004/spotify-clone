
import { TopNav } from "@/components/TopNav";
import { LibrarySidebar } from "@/components/LibrarySidebar";
import { HomeFeed } from "@/components/HomeFeed";
import { Player } from "@/components/Player";
import { MainLayout } from "@/components/MainLayout";
import { dehydrate, HydrationBoundary, QueryClient } from "@tanstack/react-query";
import { DAILY_MIXES } from "@/lib/mockData";

export default async function Home() {
  // Masterclass: Prefetching data on the server for instant hydration
  const queryClient = new QueryClient();

  await queryClient.prefetchQuery({
    queryKey: ["daily-mixes"],
    queryFn: async () => DAILY_MIXES, // Mocking API call for prefetch demo
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <MainLayout
        topNav={<TopNav />}
        leftSidebar={<LibrarySidebar />}
        player={<Player />}
      >
        <HomeFeed />
      </MainLayout>
    </HydrationBoundary>
  );
}
