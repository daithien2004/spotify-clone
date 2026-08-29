"use client";

import React, { useRef, useCallback, useEffect, memo } from "react";

const STORAGE_KEY = "spotify-layout-widths";
const DEFAULT_LEFT = 300;
const DEFAULT_RIGHT = 340;

interface MainLayoutProps {
  topNav: React.ReactNode;
  leftSidebar: React.ReactNode;
  rightSidebar?: React.ReactNode;
  children: React.ReactNode;
  player: React.ReactNode;
  previewBar?: React.ReactNode;
}

export const MainLayout = memo(function MainLayout({
  topNav,
  leftSidebar,
  rightSidebar,
  children,
  player,
  previewBar,
}: MainLayoutProps) {
  // Figma: sidebar 351 / feed ~682 / panel phải 355 (tại 1387px thiết kế)
  const leftWidth = useRef(DEFAULT_LEFT);
  const rightWidth = useRef(DEFAULT_RIGHT);
  const containerRef = useRef<HTMLDivElement>(null);
  // Handler đang active — để dọn listener nếu component unmount giữa lúc kéo.
  const cleanupRef = useRef<(() => void) | null>(null);

  const persistWidths = useCallback(() => {
    try {
      window.localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ left: leftWidth.current, right: rightWidth.current })
      );
    } catch {
      // localStorage không khả dụng (chế độ private/ẩn) → bỏ qua, chỉ mất tính năng nhớ.
    }
  }, []);

  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw) as { left?: number; right?: number };
      if (typeof parsed.left === "number" && Number.isFinite(parsed.left)) {
        leftWidth.current = parsed.left;
        containerRef.current?.style.setProperty("--left-w", `${parsed.left}px`);
      }
      if (typeof parsed.right === "number" && Number.isFinite(parsed.right)) {
        rightWidth.current = parsed.right;
        containerRef.current?.style.setProperty("--right-w", `${parsed.right}px`);
      }
    } catch {
      // JSON hỏng → giữ kích thước mặc định.
    }
  }, []);

  useEffect(() => () => cleanupRef.current?.(), []);

  const startResize = useCallback(
    (side: "left" | "right") => {
      return (e: React.MouseEvent) => {
        e.preventDefault();
        const startX = e.clientX;
        const startWidth = side === "left" ? leftWidth.current : rightWidth.current;

        const onMouseMove = (moveEvent: MouseEvent) => {
          const delta = moveEvent.clientX - startX;
          const newWidth = side === "left" ? startWidth + delta : startWidth - delta;

          const min = side === "left" ? 220 : 260;
          const max = side === "left" ? 480 : 440;
          const clamped = Math.min(Math.max(newWidth, min), max);

          if (side === "left") {
            leftWidth.current = clamped;
            containerRef.current?.style.setProperty("--left-w", `${clamped}px`);
          } else {
            rightWidth.current = clamped;
            containerRef.current?.style.setProperty("--right-w", `${clamped}px`);
          }
        };

        const cleanup = () => {
          document.removeEventListener("mousemove", onMouseMove);
          document.removeEventListener("mouseup", onMouseUp);
          document.body.style.cursor = "default";
          cleanupRef.current = null;
          persistWidths();
        };
        const onMouseUp = () => cleanup();

        cleanupRef.current = cleanup;
        document.addEventListener("mousemove", onMouseMove);
        document.addEventListener("mouseup", onMouseUp);
        document.body.style.cursor = "col-resize";
      };
    },
    [persistWidths]
  );

  return (
    <div
      ref={containerRef}
      className="flex h-screen flex-col bg-background text-foreground overflow-hidden font-sans selection:bg-accent-primary/30"
      style={
        {
          "--left-w": "300px",
          "--right-w": "340px",
        } as React.CSSProperties
      }
    >
      {topNav}

      <main className="flex-1 flex overflow-hidden px-2 pb-2 gap-2">
        {/* Sidebar trái */}
        <div
          style={{ width: "var(--left-w)" }}
          className="hidden lg:block shrink-0 h-full"
        >
          {leftSidebar}
        </div>

        {/* Handle trái */}
        <div
          onMouseDown={startResize("left")}
          className="hidden lg:block w-1.5 cursor-col-resize hover:bg-text-secondary/20 active:bg-accent-primary/50 transition-colors rounded-full my-4"
        />

        {/* Feed chính */}
        <div className="flex-1 min-w-0 h-full">
          {children}
        </div>

        {/* Handle phải */}
        <div
          onMouseDown={startResize("right")}
          className="hidden xl:block w-1.5 cursor-col-resize hover:bg-text-secondary/20 active:bg-accent-primary/50 transition-colors rounded-full my-4"
        />

        {/* Panel phải (tìm kiếm trống — Figma) */}
        {rightSidebar ? (
          <div
            style={{ width: "var(--right-w)" }}
            className="hidden xl:block shrink-0 h-full"
          >
            {rightSidebar}
          </div>
        ) : null}
      </main>

      {previewBar}
      {player}
    </div>
  );
});