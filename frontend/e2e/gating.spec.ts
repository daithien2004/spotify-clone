import { test, expect } from "@playwright/test";

// Chỉ cần FE dev server: middleware gate những trang cần login.
test.describe("gating khi chưa đăng nhập", () => {
  test("/account bị redirect về /login", async ({ page }) => {
    await page.goto("/account");
    await expect(page).toHaveURL(/\/login/);
  });

  test("/playlist/[id] bị redirect về /login", async ({ page }) => {
    await page.goto("/playlist/10000000-0000-4000-8000-000000000001");
    await expect(page).toHaveURL(/\/login/);
  });
});