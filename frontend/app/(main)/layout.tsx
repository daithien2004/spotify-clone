import { MainLayout } from "@/components/MainLayout";
import { TopNav } from "@/components/TopNav";
import { LibraryNav } from "@/components/LibraryNav";
import { FriendActivity } from "@/components/FriendActivity";
import { Player } from "@/components/Player";

/** Layout màn "main" — Player nằm ở layout để persist giữa các route. */
export default function MainLayoutRoute({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <MainLayout
      topNav={<TopNav />}
      leftSidebar={<LibraryNav />}
      rightSidebar={<FriendActivity />}
      player={<Player />}
    >
      {children}
    </MainLayout>
  );
}