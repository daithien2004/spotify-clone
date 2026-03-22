"use client";

import React, { useRef, useCallback, memo } from "react";

interface MainLayoutProps {
  topNav: React.ReactNode;
  leftSidebar: React.ReactNode;
  children: React.ReactNode;
  player: React.ReactNode;
}

export const MainLayout = memo(function MainLayout({ topNav, leftSidebar, children, player }: MainLayoutProps) {
  const leftWidth = useRef(280);
  const rightWidth = useRef(350);
  const containerRef = useRef<HTMLDivElement>(null);

  const startResize = useCallback((side: "left" | "right") => {
    return (e: React.MouseEvent) => {
      e.preventDefault();
      const startX = e.clientX;
      const startWidth = side === "left" ? leftWidth.current : rightWidth.current;

      const onMouseMove = (moveEvent: MouseEvent) => {
        const delta = moveEvent.clientX - startX;
        const newWidth = side === "left" ? startWidth + delta : startWidth - delta;

        const min = side === "left" ? 180 : 250;
        const max = side === "left" ? 480 : 450;
        const clamped = Math.min(Math.max(newWidth, min), max);

        if (side === "left") {
          leftWidth.current = clamped;
          containerRef.current?.style.setProperty("--left-w", `${clamped}px`);
        } else {
          rightWidth.current = clamped;
          containerRef.current?.style.setProperty("--right-w", `${clamped}px`);
        }
      };

      const onMouseUp = () => {
        document.removeEventListener("mousemove", onMouseMove);
        document.removeEventListener("mouseup", onMouseUp);
        document.body.style.cursor = "default";
      };

      document.addEventListener("mousemove", onMouseMove);
      document.addEventListener("mouseup", onMouseUp);
      document.body.style.cursor = "col-resize";
    };
  }, []);

  return (
    <div 
      ref={containerRef}
      className="flex h-screen flex-col bg-background text-foreground overflow-hidden font-sans selection:bg-green-500/30"
      style={{
        "--left-w": "280px",
        "--right-w": "350px",
      } as React.CSSProperties}
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
          className="hidden lg:block w-1.5 cursor-col-resize hover:bg-muted-foreground/20 active:bg-green-500/50 transition-colors rounded-full my-4"
        />

        {/* Feed chính */}
        <div className="flex-1 min-w-0 h-full">
          {children}
        </div>

        {/* Handle phải */}
        <div
          onMouseDown={startResize("right")}
          className="hidden xl:block w-1.5 cursor-col-resize hover:bg-muted-foreground/20 active:bg-green-500/50 transition-colors rounded-full my-4"
        />
      </main>
      {player}
    </div>
  );
});
