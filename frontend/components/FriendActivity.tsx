"use client";

import { useRouter } from "next/navigation";
import Image from "next/image";
import { NAV_USER } from "@/lib/musicData";

/** Panel phải "Friend Activity" (Figma Home 124:2941). */
export function FriendActivity() {
  const router = useRouter();

  return (
    <aside className="flex h-full flex-col rounded-lg bg-bg-elevated shadow-2xl">
      <div className="px-5 pt-6">
        <h2 className="text-base font-bold text-text-primary">
          Friend Activity
        </h2>
      </div>

      <div className="flex-1 space-y-6 px-5 py-8">
        <p className="text-sm text-text-muted">
          Let friends and followers on Spotify see what you&apos;re listening
          to.
        </p>

        <div className="flex items-center gap-3">
          <Image
            src="/figma/icon-account.png"
            alt=""
            width={36}
            height={36}
            className="h-9 w-9 rounded-full object-cover"
            aria-hidden
          />
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-text-primary">
              {NAV_USER}
            </p>
          </div>
        </div>

        <p className="text-sm text-text-muted">
          Go to{" "}
          <button
            type="button"
            className="text-text-primary underline underline-offset-2 hover:text-accent-primary"
          >
            Settings
          </button>{" "}
          &gt; Social and enable &quot;Let others see what I&apos;m
          listening to.&quot;
        </p>

        <button
          type="button"
          onClick={() => router.push("/settings")}
          className="w-full rounded-full border border-text-soft px-4 py-3 text-sm font-bold text-text-soft transition-colors hover:border-text-primary hover:text-text-primary"
        >
          SETTINGS
        </button>
      </div>
    </aside>
  );
}