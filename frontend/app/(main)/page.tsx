import HomeFeed from "@/components/HomeFeed";
import {
  dehydrate,
  HydrationBoundary,
  QueryClient,
} from "@tanstack/react-query";
import { HOME_SECTIONS } from "@/lib/musicData";
import { queryKeys } from "@/lib/queryKeys";

export default async function Home() {
  // Prefetch demo: render cần data section đầu ngay trên server.
  const queryClient = new QueryClient();

  await queryClient.prefetchQuery({
    queryKey: queryKeys.home.sections(),
    queryFn: async () => HOME_SECTIONS,
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <HomeFeed />
    </HydrationBoundary>
  );
}