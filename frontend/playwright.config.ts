import { defineConfig, devices } from "@playwright/test";

/**
 * E2E — 2 nhóm:
 *  - gating.spec.ts : chỉ cần FE dev server (middleware gate chưa login).
 *  - auth.spec.ts   : cần full stack (docker + 4 BE service + gateway) —
 *                     chạy với E2E_FULL=1, tự skip nếu thiếu env.
 * FE dev server tự khởi động qua webServer (reuse nếu đã chạy sẵn).
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  retries: process.env.CI ? 2 : 0,
  reporter: [["list"]],
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
  },
  webServer: {
    command: "npm run dev",
    url: "http://localhost:3000",
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});