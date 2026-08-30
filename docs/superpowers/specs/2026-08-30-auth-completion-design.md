# Auth Completion — Design (2026-08-30)

Hoàn thiện phần auth cả FE (UI + logic) lẫn BE, trên cả 4 mảng đã chốt với chủ dự án:

1. Reset password full flow
2. Email verification
3. Profile / Account settings
4. TOTP 2FA (chỉ local account)

Kèm cleanup các lỗ hổng phát hiện khi khảo sát.

## 1. Context / Hiện trạng

`auth-service` (8081) đã có BE khá đầy đủ, nhưng flow bị vỡ ở chỗ nối FE và thiếu vài piece:

| Mảng | BE hiện tại | FE hiện tại | Kết luận |
|---|---|---|---|
| **Reset password** | `POST /auth/forgot-password` + `POST /auth/reset-password` (Redis token 15ph, revoke-all sau reset) | `/forgot-password` là **form tĩnh, không onSubmit**; **không có route `/reset-password`** | 🔴 Flow vỡ giữa chừng: email báo `{baseUrl}/reset-password?token=` nhưng page không tồn tại |
| **Email verification** | `POST /auth/send-verification` + `POST /auth/verify-email` (Redis 24h, single-use); `User.isVerified`; nhưng **register KHÔNG auto-gửi mail** | Không có UI verify, không có trạng thái hiển thị | 🟡 BE đủ, FE + auto-send còn thiếu |
| **Profile/Account** | `User.updateProfile()` tồn tại nhưng **không có endpoint** | TopNav dropdown (Account/Profile/Settings/Private session) **chết** — không route, không handler | 🟡 Cần endpoint + UI |
| **TOTP 2FA** | Chỉ config beans `TotpConfig` (SecretGenerator/QrGenerator/CodeVerifier) — **không use case, không endpoint, không DB column** | Không gì | 🔴 Nửa vời (được PROJ_STATUS ghi nhầm là xong) |
| **Cookie consistency** | `AuthController.setAuthCookies`: httpOnly, path `/`, `domain=cookieDomain`, SameSite Lax | — | ⚪ `OAuth2SuccessHandler` không set `domain` + refresh cookie path `/api/v1/auth/refresh` lệch |
| **FE validation** | — | Chỉ HTML `required`/`minLength`, không validate logic | 🟡 Thêm helper validate thuần |
| **Register → Google** | OK | Register page `SocialButton` **không có `onClick`** | 🟡 Wire cho giống login page |
| **Auth-state bootstrap** | — | `useAuthStore` persist localStorage, không revalidate `/me` sau hydrate → token chết nhưng UI vẫn hiện avatar | 🟡 Cần bootstrap `/me` |

## 2. Goals / Non-Goals

### Goals
- 4 mảng trên hoàn chỉnh end-to-end (BE endpoint + FE UI/logic + nối qua gateway 9000).
- Tuân thủ Clean Architecture (auth-service): domain/application/infrastructure/presentation, use-case-per-feature, no Spring imports trong domain.
- Mọi logic mới có test (TDD); FE helper validate thuần được test.

### Non-Goals (đã chốt với chủ dự án)
- 2FA chỉ áp dụng cho **local account**. OAuth2 Google giữ nguyên flow 1 bước.
- Avatar = **URL text** (PATCH nhận string) — KHÔNG upload file lên MinIO, không mở rộng track-service/infra.
- Email dùng **SMTP Gmail thật** (Spring Mail đã cấu hình). Unit test mock `EmailPort`; E2E thủ công cần `MAIL_USERNAME`/`MAIL_PASSWORD` (Gmail app password) tự cấu hình.
- Không thêm lib FE mới (react-hook-form v.v.) — dùng helper validate thuần (pure functions) theo conventions.
- KHÔNG làm: link/unlink OAuth2 account, remember-me chọn lựa, WebAuthn/FIDO2, password strength meter > confirm-password, blacklist email, `/logout` all devices (BE đã có revoke qua reset; không thêm endpoint mới).

## 3. Quyết định thiết kế (ADR)

### D1 — MFA challenge token dùng Redis qua `SecurityTokenPort`
Login 2 bước cần "pending login" state. **Chọn:** dùng lại `SecurityTokenPort` (đã có Redis adapter) với `TOKEN_TYPE = "MFA_CHALLENGE"`, TTL 5 phút, single-use (xóa khi dùng).
- **Alternative đã cân:** JWT stateless có claim purpose — phức tạp hơn, khó revoke, không cần thiết; Redis port đã test sẵn (reset/verify dùng chung).

### D2 — Secret TOTP lưu DB theo 2 bước enroll→activate
`POST /2fa/enroll` sinh secret + trả QR, **lưu `totp_secret` ngay** nhưng `totp_enabled=false`. `POST /2fa/verify` (authenticated, body: code) → verify `CodeVerifier` khớp → `totp_enabled=true`. `POST /2fa/disable` (authenticated, verify code hiện tại) → xóa secret + off.
- **Vì sao enroll lưu ngay:** nếu không lưu, trang /account reload giữa chừng sẽ mất secret đã scan → trải nghiệm tệ. Bật `totp_enabled` chỉ khi đã verify thành công.

### D3 — Login 2 bước thay vì "2FA sau khi đã đăng nhập"
Khi `totp_enabled=true` → `LoginUseCase` **không** tạo refresh token/cookie, thay vào đó trả `{ mfaRequired: true, mfaToken }`. FE chuyển form nhập code → `POST /2fa/verify-login { mfaToken, code }` → sinh token + set cookie bình thường.
- **Vì sao:** cookie-based session hiện tại (auth-token/refresh-token) không có khái niệm "bán đăng nhập"; tách bước rõ ràng, không để JWT tồn tại trước khi xác nhận 2FA. State là mfaToken Redis → revoke được, TTL ngắn.

### D4 — `/auth/me` trả thêm trạng thái, profile update là `PATCH /me`
`GET /auth/me` (đã có) bổ sung `emailVerified` + `twoFactorEnabled` để FE hiển thị banner/status. Profile update = `PATCH /api/v1/auth/me` — PATCH theo REST (cập nhật một phần) đúng convention §2.

### D5 — Cookie OAuth2 đồng bộ
`OAuth2SuccessHandler` build cookie giống `AuthController` (cùng `domain` từ `app.cookie-domain`, refresh cookie `path=/`). DRY: tách helper build cookie (hiện controller có private method riêng) → shared component.

### D6 — Register auto-send verification email
`RegisterUseCase` sau khi save user + publish `UserRegistered` → gọi `EmailPort.sendVerificationEmail` (reuse logic `RequestEmailVerificationUseCase`: token EMAIL_VERIFICATION TTL 24h). FE sau register hiện banner "check your email".
- **Giữ cũ:** không chặn login khi chưa verify (notification-only).

### D7 — Bootstrap `/me` trên FE
Root layout/main layout dùng client wrapper: mount → `GET /auth/me`; success → `setAuth`; 401/none → `clearAuth`. Fix stale localStorage khi JWT chết. Không fetch trong SSR (cookie HttpOnly không đọc được).

## 4. Kiến trúc / Component

```
frontend                                   backend (auth-service 8081)
─────────────────────────                  ──────────────────────────────
app/(auth)/forgot-password  ├─────────→   POST /api/v1/auth/forgot-password
app/(auth)/reset-password   ├─────────→   POST /api/v1/auth/reset-password
app/(auth)/verify-email     ├─────────→   POST /api/v1/auth/verify-email
app/(auth)/login            ├─────────→   POST /api/v1/auth/login    (+ 2FA: mfaRequired)
app/(auth)/login (2FA step) ├─────────→   POST /api/v1/auth/2fa/verify-login
app/(main)/account          ├─────────→   PATCH /api/v1/auth/me   (profile)
app/(main)/account          ├─────────→   GET  /api/v1/auth/me   (+ emailVerified/2faEnabled)
app/(main)/account (2FA)    ├─────────→   POST /api/v1/auth/2fa/{enroll,verify,disable}
hooks/useAuth (thêm hooks)  │             application/usecase/ (mới PATCH me + 2FA x4)
services/api/authService    │             domain/entity/User (+ tots secret, 2faEnabled)
lib/api-client              │             infrastructure/security/Totp (beans đã có)
middleware.ts (whitelist)   │             infrastructure/persistence (mapper + migration V2)
```

### Data flow — Login 2 bước
```
[FE /login] ─ login(email, pass) ─→ [BE LoginUseCase]
      │ password OK, totp_enabled=false → set cookie, return user   (không đổi)
      │ password OK, totp_enabled=true  → KHÔNG cookie
      ▼
      return { success, mfaRequired: true, mfaToken, userId, email, ... , twoFactorEnabled: true }
[FE] chuyển form nhập 6-chữ-số (giữ mfaToken trong state/ref của component)
[FE] ─ verify-login(mfaToken, code) ─→ [BE Verify2faLoginUseCase]
      → findByToken(MFA_CHALLENGE) → verify CodeVerifier(code, user.totpSecret)
      → đúng: delete token, tạo access+refresh, set cookie, return user; sai: DomainException("Invalid 2FA code")
```

### Data flow — Reset password
```
[FE /forgot-password] submit → POST /forgot-password → BE lưu Redis 15ph + gửi mail link
[email] link = {app.base-url}/reset-password?token=...
[FE /reset-password?token=] form mật khẩu mới (validate ≥8 + confirm) → POST /reset-password
[BE] token hợp lệ → đổi pass + delete token + revoke-all refresh + audit
[FE] thành công → redirect /login (BE đã force logout mọi thiết bị)
   token hết hạn/đã dùng → BE DomainException → FE hiển thị "link hết hạn, yêu cầu lại"
```

### Data flow — Email verification
```
[REGISTER] → [BE RegisterUseCase] → save + publish UserRegistered + sendVerificationEmail(link)
[email] link = {app.base-url}/verify-email?token=...
[FE /verify-email?token=] mount → POST /verify-email → BE verify (single-use) → FE success + redirect
[FE /account] banner "Email chưa xác minh" + [Gửi lại] → POST /send-verification
```

### Data flow — 2FA setup (FE /account)
```
[Off] → [Bật 2FA] → POST /2fa/enroll → { otpauthUrl, qrDataUri, secret(optional) }
  FE render QR + hướng dẫn → user scan bằng app (Google Authenticator)
  user nhập code → POST /2fa/verify { code } → BE đúng → totp_enabled=true → UI về status ON
[On] → [Tắt 2FA] → prompt nhập code → POST /2fa/disable { code } → xóa secret + off
```

## 5. Backend — chi tiết

### 5.1 Migration (Flyway)
**`backend/auth-service/src/main/resources/db/migration/V2__add_2fa_and_profile.sql`**
```sql
ALTER TABLE users
    ADD COLUMN totp_secret     VARCHAR(255),
    ADD COLUMN totp_enabled    BOOLEAN NOT NULL DEFAULT FALSE;
```

### 5.2 Domain
`User` entity thêm field `totpSecret`, `totpEnabled` (+ behavior):
- `enable2fa(String secret)` → set secret + true
- `verify2faCode(...)` KHÔNG ở domain (phụ thuộc thư viện) — tách ra port `TotpPort`
- `disable2fa()` → xóa secret + false
- `isTwoFactorEnabled()`

### 5.3 Port mới
- `application/port/out/TotpPort.java`:
  - `String generateSecret()`
  - `String generateQrDataUri(String account, String issuer, String secret)` (base64 PNG data URI)
  - `String buildOtpAuthUri(String account, String issuer, String secret)`
  - `boolean isValid(String code, String secret)`
  - Adapter `infrastructure/security/TotpAdapter.java` dùng beans `TotpConfig` đã có.

### 5.4 Use cases mới (`application/usecase/`)
1. `UpdateProfileUseCase` — `PATCH /me`: `Request(displayName, avatarUrl)` — **cả 2 optional, ít nhất 1 non-null** (PATCH phần); gọi `user.updateProfile`, return UserResponse mới.
2. `EnrollTwoFactorUseCase` — `Request()`: user **chưa bật** (đã bật → DomainException "2FA already enabled"); sinh secret + lưu `totpSecret` (chưa bật); return `{ otpauthUrl, qrDataUri }` (QR = base64 PNG data URI, không secret dạng text).
3. `VerifyTwoFactorSetupUseCase` — `Request(code)` (`@Size(min=6,max=6)`): user **đã có secret** (chưa enroll → DomainException); `totpPort.isValid` → `user.enable2fa`; else DomainException("Invalid 2FA code").
4. `DisableTwoFactorUseCase` — `Request(code)`: verify code với secret; đúng → `user.disable2fa`, xóa mọi MFA_CHALLENGE của user (nếu có); sai → DomainException.
5. `VerifyTwoFactorLoginUseCase` — `Request(mfaToken, code)`: findByToken(MFA_CHALLENGE) → get user → verify code → tạo access/refresh + save refresh token + set cookie? **KHÔNG — use case không nhận HttpServletResponse** (Clean Arch). Return Response(accessToken, refreshToken, user info) như LoginUseCase; controller set cookie.
   - delete MFA_CHALLENGE token sau khi dùng; đúng → issue tokens như Login; sai → DomainException, KHÔNG xóa token (retry).

### 5.5 Modify use case hiện có
- `LoginUseCase`: thêm cuối — nếu `user.isTwoFactorEnabled()` → sinh mfaToken (SecurityTokenPort, MFA_CHALLENGE, TTL 5min) → trả `Response(..., mfaRequired=true, mfaToken, twoFactorEnabled=true)`; KHÔNG tạo refresh token; KHÔNG publish LOGIN_SUCCESS. Response record thêm field.
- `RegisterUseCase`: sau `domainEventPublisher.publish(...)` → auto-send verification: tạo token EMAIL_VERIFICATION TTL 24h + `emailPort.sendVerificationEmail(...)`.
- `GetCurrentUserUseCase`: Response thêm `emailVerified`, `twoFactorEnabled` (lấy từ `user.isVerified()`, `user.isTwoFactorEnabled()`).
- `AuthController`: 
  - `PATCH /me` + `@SecurityRequirements()`? PATCH /me là protected (qua gateway authFilter) nhưng nằm trong `authSecurityFilterChain` matcher `/api/v1/auth/**` — nhưng chain hiện `.anyRequest().authenticated()` và `.addFilterBefore(gatewayHeaderFilter...)`. Gateway filter set `X-User-Id` từ JWT. Nên PATCH /me protected như `/me` (đọc header). 
  - Thêm 4 2FA endpoints.

### 5.6 Security config
- `SecurityConfig.authSecurityFilterChain`: hiện `.requestMatchers(login/register/refresh/error).permitAll()`, `.anyRequest().authenticated()`. 2FA endpoints → authenticated (user trong cookie/JWT qua gateway). `/2fa/verify-login` KHÔNG có JWT nhưng cần authorize bằng mfaToken — để `permitAll`? Nếu để authenticated sẽ bị chặn (chưa có cookie). **Đặt `/api/v1/auth/2fa/verify-login` vào permitAll** (giống refresh) — security nằm ở mfaToken single-use 5ph. Còn enroll/verify/disable → authenticated.
  - Lưu ý gateway route: `/api/v1/auth/**` ngoài login/register/refresh sẽ qua `authFilter` (JWT). `verify-login` bị gateway chặn nếu không có JWT! → gateway route cũng cần permit `/api/v1/auth/2fa/verify-login`.

### 5.7 Gateway
`GatewayConfig`:
- Route `auth-service-login` (bypass filter) thêm path `/api/v1/auth/2fa/verify-login`.
- (PATCH /me, 2FA enroll/verify/disable đi qua authFilter bình thường.)

### 5.8 Persistence mapper
`UserJpaMapper`/`UserJpaEntity` thêm `totpSecret`, `totpEnabled` map.

## 6. Frontend — chi tiết

### 6.1 Routes & pages
- `/login` — thêm **2FA step**: `useLogin` trả `{ mfaRequired, mfaToken }` → state `step` trong component; render form 6-chữ số; submit → `useVerify2faLogin`.
- `/forgot-password` — wire: `useForgotPassword` (mutation), validate email, success state (thông báo "check your email"), error toast.
- `/reset-password` (MỚI) — đọc `token` từ `useSearchParams`; form newPassword + confirmPassword (validate); `useResetPassword`; pending/error/success states; link "gửi lại email".
- `/verify-email` (MỚI) — mount `useEffect` → `AuthService.verifyEmail(token)`; success page + "tiếp tục" (đi /; login-page nếu chưa login); error page (link hết hạn → gửi lại).
- `/account` (MỚI, `(main)`) — profile form (displayName, avatarUrl URL) + **email status banner** (+ resend verification) + **2FA section** (status, enroll QR, verify code, disable). Các mục TopNav dropdown **Account/Profile/Settings → `/account`**; mục "Private session" giữ nguyên trang trí (visual-only, ngoài scope).

### 6.2 Hooks mới (`hooks/useAuth.ts` hoặc tách `hooks/useAuthExtended.ts`)
- `useForgotPassword`, `useResetPassword`, `useVerifyEmail`, `useUpdateProfile`, `useEnable2fa` (enroll→verify), `useDisable2fa`, `useVerify2faLogin`, `useResendVerification`.
- `useBootstrapAuth` — root layout: mount → `me()` → setAuth/clearAuth.

### 6.3 Services (`services/api/authService.ts`)
Thêm: `forgotPassword`, `resetPassword`, `sendVerification`, `verifyEmail`, `updateProfile (PATCH me)`, `enroll2fa`, `verify2faSetup`, `disable2fa`, `verify2faLogin`. `me()` response type thêm `emailVerified`, `twoFactorEnabled`.

### 6.4 Types (`types/auth.ts`)
- `AuthResponse` thêm optional `mfaRequired: boolean`, `mfaToken: string`, `twoFactorEnabled: boolean`, `emailVerified: boolean`.
- `ProfileResponse` thêm `emailVerified`, `twoFactorEnabled`.

### 6.5 Middleware
Whitelist public thêm `/reset-password`, `/verify-email` (đã + `/forgot-password`? hiện `isAuthPage` chỉ login/register — bổ sung forgot-password, reset-password, verify-email).

### 6.6 Validate thuần (TDD) — `lib/validation/auth.ts`
- `validateEmail`, `validatePassword` (≥8), `validateDisplayName` (non-blank), `validateConfirmPassword`, `validateTotpCode` (6 digits). Trả `{ valid, message }` hoặc error map. Được dùng bởi form login/register/forgot/reset/2FA.

### 6.7 Register page
- Wire `SocialButton` `onClick` → redirect `{GATEWAY_URL}/oauth2/authorization/google` (giống login).

## 7. Error handling & security

- TOTP sai code → `DomainException("Invalid 2FA code")` → 400, FE toast. **Rate limit:** `RateLimitingFilter` có sẵn — kiểm tra áp cho `/2fa/verify-login` (brute-force 6-chữ-số cần giới hạn; mfaToken TTL 5ph giúp chặn vét cạn, nhưng vẫn nên rate-limit).
- MfaToken sai/đã dùng/hết hạn → 400 "2FA session expired, please log in again".
- Đăng nhập thành công 2FA → ghi LOGIN_SUCCESS audit; reset `failedLoginAttempts`.
- Verify-login KHÔNG xóa token khi code sai (cho phép retry 1-2 lần trước khi hết TTL).
- Reset password xong → FE bắt buộc redirect login (không auto-login) vì BE revoke-all.
- Cookie: secure=true khi production (giữ nguyên pattern hiện tại - để false local).
- Email enumeration: giữ nguyên anti-enumeration (forgot-password luôn trả success). Verify-email vẫn trả lỗi token cụ thể vì token là possession factor.

## 8. Testing

### BE (TDD — test trước, gate `mvn test -pl auth-service -am`)
- `UpdateProfileUseCaseTest` — cập nhật thành công / user không tồn tại.
- `LoginUseCaseTest` — nhánh mới: 2FA on → mfaRequired=true + mfaToken sinh, no cookie token; 2FA off → như cũ.
- `EnrollTwoFactorUseCaseTest` — 2 lần enroll bị chặn (nếu đã bật), trả QR/otpauth.
- `VerifyTwoFactorSetupUseCaseTest` — code đúng → enabled; sai → DomainException.
- `DisableTwoFactorUseCaseTest` — code đúng → off + secret null; sai → DomainException.
- `VerifyTwoFactorLoginUseCaseTest` — mfaToken hợp lệ + code đúng → tokens; sai code → fail, token giữ nguyên; token sai/hết hạn → DomainException.
- `RegisterUseCaseTest` — bổ sung verify emailPort.sendVerificationEmail gọi 1 lần.
- `GetCurrentUserUseCaseTest` — trả emailVerified/twoFactorEnabled.
- Test migration: (không có test tự động cho Flyway SQL; verify bằng boot thật trong smoke).
- Clean Arch self-check: domain không import Spring.

### FE
- `lib/validation/__tests__/auth.test.ts` — validate helpers.
- `hooks/__tests__/useAuth.test.ts(x)` — useLogin với mfaRequired, useVerify2faLogin, useForgotPassword/reset, useUpdateProfile, useEnable2fa.
- `components/auth/__tests__/Login2faStep.test.tsx` — nhập code → gọi verify-login.
- `services/api/__tests__/authService.test.ts` — 9 method mới gọi đúng path/params.
- Page test (server/client render) cho `reset-password` (token missing/hết hạn) + `verify-email`.
- Gate: `npx tsc --noEmit` → `npm run lint` → vitest (batch paths) → `npm run build` (nếu structural).

### E2E thủ công (smoke, spec §9 checklist — không tự động)
- Register (SMTP Gmail thật) → nhận mail verify → click link `/verify-email` → banner mất.
- Forgot → mail reset → `/reset-password` → đổi pass → login lại thành công; token cũ bị revoke.
- Bật 2FA trên /account (scan QR, nhập code) → logout → login → nhập code → vào app.
- Sai code nhiều lần → fail có kiểm soát.

## 9. Out of scope / tradeoffs (nhắc lại)
- User-service còn backlog (profile/follows) — mảng Account ở đây CHỈ profile trong auth-service.
- Avatar URL-only; không MinIO upload (track-service đã có MinIO cho track audio, không dùng cho avatar đợt này).
- 2FA local-only; Google giữ nguyên.
- Chưa tách `user-service` — PATCH /me nằm ở auth-service (quyết định giữ trong bounded context Auth vì user-service chưa tồn tại).

## 10. Rủi ro / phụ thuộc
- SMTP Gmail thật cần app-password; nếu chưa cấu hình → E2E email bị chặn (unit test vẫn xanh vì mock).
- `CodeVerifier` của `dev.samstevens.totp` beans đã có; mount QR cần `ZxingPngQrGenerator` (đã có) — không thêm dependency backend.
- FE 2FA step: cần giữ mfaToken an toàn (chỉ trong memory state, không persist, không in log).