"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { Home, Library, Plus, Heart } from "lucide-react";
import { cn } from "@/lib/utils";
import { PLAYLISTS } from "@/lib/musicData";
import { queryKeys } from "@/lib/queryKeys";
import {
  PlaylistService,
  type PlaylistSummaryItem,
} from "@/services/api/playlistService";

/** Sidebar trái (Figma "Spotify Music UI Design (Community)" 124:2941). */

const NAV_ITEMS = [
  {
    label: "Home",
    href: "/",
    icon: Home,
    match: (path: string) => path === "/",
  },
  {
    label: "Your Library",
    href: "/",
    icon: Library,
    match: (path: string) => path.startsWith("/playlist"),
  },
] as const;

/** Khi API playlist không tới được (offline demo) — sidebar vẫn có nội dung. */
function fallbackSummaries(): PlaylistSummaryItem[] {
  return Object.values(PLAYLISTS)
    .slice(0, 3)
    .map((p) => ({ id: p.id, title: p.title, owner: p.owner, coverUrl: p.coverUrl }));
}

export function LibraryNav() {
  const pathname = usePathname();

  const playlistsQuery = useQuery({
    queryKey: queryKeys.playlists.list(),
    queryFn: PlaylistService.listPlaylists,
    staleTime: 60_000,
  });

  const playlists =
    playlistsQuery.isError ? fallbackSummaries() : playlistsQuery.data ?? [];

  return (
    <aside className="flex h-full flex-col gap-2">
      <nav
        className="rounded-lg bg-bg-elevated p-2 shadow-2xl"
        aria-label="Primary"
      >
        <ul className="space-y-1">
          {NAV_ITEMS.map(({ label, href, icon: Icon, match }) => {
            const active = match(pathname);
            return (
              <li key={label}>
                <Link
                  href={href}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "flex items-center gap-4 rounded-md px-3 py-2.5 text-[15px] font-bold text-text-hint transition-colors hover:text-text-primary",
                    active && "text-text-primary"
                  )}
                >
                  <Icon className="h-6 w-6 shrink-0" />
                  {label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      <div className="flex min-h-0 flex-1 flex-col rounded-lg bg-bg-elevated p-2 shadow-2xl">
        <div className="px-3 pb-2 pt-2">
          <div className="flex items-center gap-1">
            <button
              type="button"
              aria-label="Create playlist or folder"
              className="flex h-9 w-9 items-center justify-center rounded-full text-text-soft transition-colors hover:bg-white/10 hover:text-text-primary"
            >
              <Plus className="h-5 w-5" />
            </button>
            <Link
              href="/"
              className="rounded-full px-3 py-2 text-sm font-bold text-text-soft transition-colors hover:bg-white/10 hover:text-text-primary"
            >
              Create Playlist
            </Link>
          </div>
          <Link
            href="/playlist/liked-songs"
            className="mt-1 flex items-center gap-1 rounded-md py-1"
          >
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-blue-400 via-purple-500 to-indigo-700 text-white">
              <Heart className="h-5 w-5 fill-current" />
            </span>
            <span className="rounded-full px-3 py-2 text-sm font-bold text-text-soft transition-colors hover:text-text-primary">
              Liked Songs
            </span>
          </Link>
        </div>

        <div className="mt-2 min-h-0 flex-1 overflow-y-auto">
          <ul className="space-y-2 px-3 pb-2">
            {playlists.map((pl) => (
              <li key={pl.id}>
                <Link
                  href={`/playlist/${pl.id}`}
                  className="group flex items-center gap-3 rounded-md p-2 transition-colors hover:bg-white/10"
                >
                  <Image
                    src={pl.coverUrl}
                    alt=""
                    width={48}
                    height={48}
                    className="h-12 w-12 shrink-0 rounded object-cover"
                    aria-hidden
                  />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-text-soft group-hover:text-text-primary">
                      {pl.title}
                    </p>
                    <p className="truncate text-xs text-text-muted">
                      Playlist · {pl.owner}
                    </p>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </aside>
  );
}