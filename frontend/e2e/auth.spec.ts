import { test, expect } from "@playwright/test";
import { execFileSync } from "node:child_process";
import path from "node:path";

// Cần full stack (docker + 4 BE service + gateway 9000 + mailpit): set E2E_FULL=1.
const GW = "http://localhost:9000/api/v1";
const MP = "http://localhost:8025/api/v1";
const TOTP_SCRIPT = path.resolve(process.cwd(), "../scripts/totp.mjs");

interface MailpitRecipient {
  Address?: string;
}
interface MailpitMessage {
  ID?: string;
  To?: MailpitRecipient[];
  HTML?: string;
}

test.describe("auth E2E", () => {
  test.skip(!process.env.E2E_FULL, "set E2E_FULL=1 khi stack đang chạy");
  // Flow dài (register + poll email + nhiều lần login + TOTP) — nới timeout khỏi mặc định 30s.
  test.setTimeout(120_000);

  const totp = (secret: string): string =>
    execFileSync("node", [TOTP_SCRIPT, secret, "30"]).toString().trim();

  // Registration gửi email async (@Async) — poll mailpit tới khi thấy link.
  // LƯU Ý: không dùng /search?query= — Mailpit trả messages:[] với query này (HTML
  // cũng 0 trong list). Lấy list /messages rồi fetch từng /message/{id} để có HTML
  // (giống scripts/smoke-auth.sh::mail_token — đã verify).
  async function verifyEmail(address: string, marker: string): Promise<string | null> {
    for (let i = 0; i < 60; i++) {
      const res = await fetch(`${MP}/messages`);
      const data = (await res.json()) as { messages?: MailpitMessage[] };
      for (const msg of data.messages ?? []) {
        if (!(msg.To ?? []).some((r) => r.Address === address)) continue;
        const detail = (await (await fetch(`${MP}/message/${msg.ID}`)).json()) as {
          HTML?: string;
        };
        const match = (detail.HTML ?? "").match(
          new RegExp(`href="[^"]*${marker}\\?token=([0-9a-fA-F-]{36})`)
        );
        if (match) return match[1];
      }
      await new Promise((r) => setTimeout(r, 1000));
    }
    return null;
  }

  test("register (API) → verify email → login (UI) → /account hiển thị displayName", async ({
    page,
    context,
  }) => {
    const email = `e2e.${Date.now()}.x@example.com`;
    const password = "E2ePass2026!x";
    const displayName = "E2E Tester";
    const api = context.request; // share cookie store với browser context

    // setup: register + verify email qua API (cookie từ register cho phép middleware)
    const reg = await api.post(`${GW}/auth/register`, {
      data: { email, password, displayName },
    });
    expect(reg.status()).toBe(201);
    const token = await verifyEmail(email, "verify-email");
    expect(token).toBeTruthy();
    const vr = await api.post(`${GW}/auth/verify-email`, { data: { token } });
    expect(vr.status()).toBe(200);

    // context.request chia sẻ cookie store với browser → register để lại auth-token,
    // khiến middleware redirect /login → / (user đã được xem là đã login). Xóa cookie
    // để /login hiện form — flow UI phải là một lần đăng nhập thật từ trang trống.
    await context.clearCookies();
    await page.goto("/login");
    await page.locator("#email").fill(email);
    await page.locator("#password").fill(password);
    await page.getByRole("button", { name: "Log in" }).click();
    await expect(page).toHaveURL(/\/$/, { timeout: 15_000 });

    await page.goto("/account");
    await expect(page.locator("#displayName")).toHaveValue(displayName);
  });

  test("username có 2FA: login hiện bước TOTP → nhập code → về home", async ({
    page,
    context,
  }) => {
    const email = `e2e.${Date.now()}.y@example.com`;
    const password = "E2ePass2026!x";
    const api = context.request;

    const reg = await api.post(`${GW}/auth/register`, {
      data: { email, password, displayName: "E2E Two" },
    });
    expect(reg.status()).toBe(201);
    const token = await verifyEmail(email, "verify-email");
    expect(token).toBeTruthy();
    await api.post(`${GW}/auth/verify-email`, { data: { token } });

    // login lần đầu (chưa 2FA) để cookie → enroll 2FA qua API.
    // Xóa cookie register trước khi vào /login (middleware redirect auth → /).
    await context.clearCookies();
    await page.goto("/login");
    await page.locator("#email").fill(email);
    await page.locator("#password").fill(password);
    await page.getByRole("button", { name: "Log in" }).click();
    await expect(page).toHaveURL(/\/$/, { timeout: 15_000 });

    const enroll = await api.post(`${GW}/auth/2fa/enroll`);
    expect(enroll.status()).toBe(200);
    const enrollData = (await enroll.json()) as {
      data: { otpauthUrl: string };
    };
    const secret = new URL(
      enrollData.data.otpauthUrl.replace("otpauth://totp/", "https://totp/")
    ).searchParams.get("secret");
    expect(secret).toBeTruthy();
    expect((await api.post(`${GW}/auth/2fa/verify`, { data: { code: totp(secret!) } })).status()).toBe(200);

    // Kết thúc session giả lập: xóa cookie là đủ (2FA giờ đã bật trên account) — login
    // lại sẽ cấp JWT mới, nên không cần gọi logout API / click nút logout UI (dễ gây
    // race context-close khi navigate giữa chừng).
    await context.clearCookies();
    await page.goto("/login");
    await page.locator("#email").fill(email);
    await page.locator("#password").fill(password);
    await page.getByRole("button", { name: "Log in" }).click();
    await expect(page.getByText("Two-factor authentication")).toBeVisible({ timeout: 15_000 });

    // placeholder "123456" là duy nhất trên bước login-TOTP (id #2fa-code đôi khi bị
    // browser querySelectorAll báo sai "not a valid selector" — dùng placeholder cho chắc).
    await page.getByPlaceholder("123456").fill(totp(secret!));
    await page.getByRole("button", { name: "Verify" }).click();
    await expect(page).toHaveURL(/\/$/, { timeout: 15_000 });
  });
});