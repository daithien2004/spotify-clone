import HomeFeed from "@/components/HomeFeed";

// Data section fetch client-side (HomeFeed) — Home public nhưng API cần JWT
// nên không thể prefetch server; HomeFeed gate theo auth + fallback mock.
export default function Home() {
  return <HomeFeed />;
}