"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { useRouter } from "next/navigation";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import Link from "next/link";
import Image from "next/image";
import { useAuthStore } from "@/hooks/useAuthStore";
import { useLogout } from "@/hooks/useAuth";
import { useSyncExternalStore } from "react";
import { SearchBar } from "@/components/search/SearchBar";

// Trạng thái "đã hydrate" — true trên client, false khi SSR.
// Tránh setState-synchronously-in-effect (cascading renders).
const emptySubscribe = () => () => {};
const getClientSnapshot = () => true;
const getServerSnapshot = () => false;

export function useIsMounted() {
  return useSyncExternalStore(emptySubscribe, getClientSnapshot, getServerSnapshot);
}

/** Header (Figma 113:707): chevron + SearchBar + avatar dropdown (menu Figma 325:7537). */
export function TopNav() {
  const mounted = useIsMounted();
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const isAuth = useAuthStore((state) => state.isAuthenticated());
  const logout = useLogout();

  const avatar = user?.avatarUrl ? (
    <Image
      src={user.avatarUrl}
      alt="Profile"
      width={32}
      height={32}
      className="h-full w-full object-cover"
    />
  ) : (
    <span className="text-sm font-bold uppercase">
      {(user?.displayName || "U").charAt(0)}
    </span>
  );

  return (
    <nav className="sticky top-0 z-10 grid h-[76px] grid-cols-[1fr_auto_1fr] items-center gap-6 px-6">
      {/* Left — back/forward chevrons */}
      <div className="flex items-center gap-2 justify-self-start shrink-0">
        <button
          type="button"
          onClick={() => router.back()}
          aria-label="Back"
          className="flex h-8 w-8 items-center justify-center rounded-full bg-black/50 text-text-soft hover:text-text-primary transition-colors"
        >
          <ChevronLeft className="h-5 w-5" />
        </button>
        <button
          type="button"
          onClick={() => router.forward()}
          aria-label="Forward"
          className="flex h-8 w-8 items-center justify-center rounded-full bg-black/50 text-text-soft hover:text-text-primary transition-colors"
        >
          <ChevronRight className="h-5 w-5" />
        </button>
      </div>

      {/* Center — search (dropdown autocomplete hiện ngay dưới) */}
      <SearchBar />

      {/* Right — avatar dropdown */}
      <div className="flex items-center gap-4 justify-self-end shrink-0">
        {mounted && isAuth ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                type="button"
                aria-label="Account"
                className="flex h-8 w-8 items-center justify-center overflow-hidden rounded-full bg-bg-tertiary/80 text-text-soft transition-transform hover:scale-105"
              >
                {avatar}
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              align="end"
              className="w-60 rounded-xl bg-bg-tertiary/95 p-1.5 shadow-2xl backdrop-blur-md"
            >
              <DropdownMenuLabel className="flex flex-col px-3 py-2">
                <span className="text-sm font-bold text-text-primary">
                  {user?.displayName}
                </span>
                <span className="text-xs text-text-muted">{user?.email}</span>
              </DropdownMenuLabel>
              <DropdownMenuSeparator className="my-1 bg-border/50" />
              <DropdownMenuItem className="cursor-pointer rounded-md px-3 py-2.5 text-sm font-medium text-text-strong hover:bg-white/10">
                Account
              </DropdownMenuItem>
              <DropdownMenuItem className="cursor-pointer rounded-md px-3 py-2.5 text-sm font-medium text-text-strong hover:bg-white/10">
                Profile
              </DropdownMenuItem>
              <DropdownMenuItem className="cursor-pointer rounded-md px-3 py-2.5 text-sm font-medium text-text-strong hover:bg-white/10">
                Private session
              </DropdownMenuItem>
              <DropdownMenuItem className="cursor-pointer rounded-md px-3 py-2.5 text-sm font-medium text-text-strong hover:bg-white/10">
                Settings
              </DropdownMenuItem>
              <DropdownMenuSeparator className="my-1 bg-border/50" />
              <DropdownMenuItem
                className="cursor-pointer rounded-md px-3 py-2.5 text-sm font-medium text-text-strong hover:bg-white/10 focus:bg-white/10"
                onClick={logout}
              >
                Log out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : mounted ? (
          <Link
            href="/login"
            className="rounded-full bg-white px-8 py-3 text-sm font-bold text-black transition-transform hover:scale-105"
          >
            Log in
          </Link>
        ) : (
          <div className="w-28 h-9" /> // Placeholder layout shift prevention
        )}
      </div>
    </nav>
  );
}