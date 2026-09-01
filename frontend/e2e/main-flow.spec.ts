import { test, expect, type Page } from "@playwright/test";

/**
 * P4 — E2E main user flow (test chuẩn chỉ).
 *
 * Flow chính của sản phẩm, chạy trên FULL stack (docker + 4 BE service + gateway
 * 9000 + mailpit + FE dev server qua webServer của playwright.config):
 *   mở playlist  →  click dòng track  →  Player phát & seek  →  search → click kết quả → phát.
 *
 * Giống auth.spec.ts: cần stack đang chạy → set E2E_FULL=1, tự skip nếu thiếu env.
 * Mỗi test tự tạo 1 account riêng (email theo timestamp) để cô lập.
 */
test.describe("main user flow E2E", () => {
  test.skip(!process.env.E2E_FULL, "set E2E_FULL=1 khi stack đang chạy");
  test.describe.configure({ mode: "serial" });
  // setup + chờ audio tải + seek → nới timeout khỏi mặc định 30s.
  test.setTimeout(120_000);

  const GW = "http://localhost:9000/api/v1";
  const MP = "http://localhost:8025/api/v1";
  const CHILL_PLAYLIST_ID = "10000000-0000-4000-8000-000000000001";

  // Seed cố định (track-service V2__seed_tracks.sql) phải tồn tại trong search index.
  const SEED_FIRST = "Play It Safe";
  const SEED_ODESZA = "A Moment Apart";

  interface MailpitMessage {
    ID?: string;
    To?: { Address?: string }[];
    HTML?: string;
  }
  interface MailpitRecipient {
    Address?: string;
  }

  /** Tạo account thật (register API) + verify email (lấy token từ Mailpit). */
  async function createVerifiedAccount(): Promise<{ email: string; password: string }> {
    const email = `flow.${Date.now()}.x@example.com`;

    const password = "E2ePass2026!x";
    const reg = await fetch(`${GW}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password, displayName: "Flow Tester" }),
    });
    expect(reg.status).toBe(201);

    // Poll Mailpit tới khi thấy link verify (async email).
    let token: string | null = null;
    for (let i = 0; i < 60; i++) {
      const list = (await (await fetch(`${MP}/messages`)).json()) as {
        messages?: MailpitMessage[];
      };
      for (const msg of list.messages ?? []) {
        if (!(msg.To ?? []).some((r: MailpitRecipient) => r.Address === email)) continue;
        const detail = (await (await fetch(`${MP}/message/${msg.ID}`)).json()) as {
          HTML?: string;
        };
        const match = (detail.HTML ?? "").match(
          new RegExp(`href="[^"]*verify-email\\?token=([0-9a-fA-F-]{36})`)
        );
        if (match) token = match[1];
      }
      if (token) break;
      await new Promise((r) => setTimeout(r, 1000));
    }
    expect(token).toBeTruthy();
    const vr = await fetch(`${GW}/auth/verify-email`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token }),
    });
    expect(vr.status).toBe(200);
    return { email, password };
  }

  /** UI login 1 lần thật: điền form → Log in → về home. */
  async function loginViaUi(page: Page, email: string, password: string): Promise<void> {
    await page.goto("/login");
    await page.locator("#email").fill(email);
    await page.locator("#password").fill(password);
    await page.getByRole("button", { name: "Log in" }).click();
    await expect(page).toHaveURL(/\/$/, { timeout: 15_000 });
  }

  test("mở Chill Mix → click track → Player phát (Pause) → seek slider tăng thời gian", async ({
    page,
  }) => {
    const { email, password } = await createVerifiedAccount();
    await loginViaUi(page, email, password);

    // Mở playlist seeded Chill Mix (id cố định trong V3__seed_playlists.sql).
    await page.goto(`/playlist/${CHILL_PLAYLIST_ID}`);
    await expect(page.getByRole("heading", { name: "Chill Mix" })).toBeVisible({
      timeout: 15_000,
    });

    // Wait track rows load từ 2 API (playlist tracks + track metadata batch).
    const firstRow = page.locator('[role="row"]', { hasText: SEED_FIRST });
    await expect(firstRow).toBeVisible({ timeout: 15_000 });

    // Click dòng track đầu → Player nạp queue + phát: nút toggle chuyển sang "Pause",
    // TrackInfo hiện title.
    await firstRow.click();
    await expect(
      page.getByRole("button", { name: "Pause" })
    ).toBeVisible({ timeout: 20_000 });
    await expect(page.locator("span", { hasText: SEED_FIRST }).first()).toBeVisible();

    // Seek: focus "Song duration" slider thumb (Radix role="slider"), nhấn ArrowRight
    // → trình phát tua thêm; aria-valuenow / currentTime phải tăng qua 0.
    const slider = page.getByRole("slider", { name: "Song duration" });
    await slider.focus();
    const before = Number(await slider.getAttribute("aria-valuenow"));
    await page.keyboard.press("ArrowRight");
    await page.keyboard.press("ArrowRight");
    const after = Number(await slider.getAttribute("aria-valuenow"));
    expect(after).toBeGreaterThan(before);
  });

  test("search 'odesza' → click kết quả → Player phát track seeded", async ({ page }) => {
    const { email, password } = await createVerifiedAccount();
    await loginViaUi(page, email, password);

    // SearchBar (combobox) trên header — gõ query, debounce 300ms → GET /search/tracks thật.
    const search = page.getByRole("combobox");
    await search.fill("odesza");
    await search.press("Enter"); // đóng dropdown không cần — click result sẽ tự đóng.

    // Kết quả từ Elasticsearch thật: track "A Moment Apart" (ODESZA) đã seed.
    const result = page.getByText(SEED_ODESZA, { exact: false }).first();
    await expect(result).toBeVisible({ timeout: 20_000 });
    await result.click();

    // Play kết quả search → Player toggle "Pause" + TrackInfo hiện title.
    await expect(
      page.getByRole("button", { name: "Pause" })
    ).toBeVisible({ timeout: 20_000 });
    await expect(page.locator("span", { hasText: SEED_ODESZA }).first()).toBeVisible();
  });
});
