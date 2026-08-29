import React from "react";
import { vi } from "vitest";
import "@testing-library/jest-dom/vitest";

// jsdom chưa triển khai ResizeObserver — Radix UI (Slider, ScrollArea) cần.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}

if (!("ResizeObserver" in globalThis)) {
  globalThis.ResizeObserver = ResizeObserverStub;
}

// next/image — mock để test chỉ render JSX (tránh pipeline optimize ảnh).
vi.mock("next/image", () => ({
  default: (props: React.ImgHTMLAttributes<HTMLImageElement>) =>
    React.createElement("img", props),
}));