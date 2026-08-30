# Auth Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện auth end-to-end (FE + BE): reset password flow, email verification, profile/account settings, TOTP 2FA (local-only) + cleanup các lỗ hổng FE.

**Architecture:** Mở rộng `auth-service` (8081) theo Clean Architecture — domain/application/infrastructure/presentation không đổi. TOTP dùng beans `dev.samstevens.totp` đã có (chỉ thêm adapter `TotpPort`), mfaToken lưu Redis qua `SecurityTokenPort` (đã có Redis adapter). FE giữ pattern hiện tại (useState + helpers validate thuần + React Query mutation hooks). Gateway thêm ngoại lệ route cho endpoints công khai.

**Tech Stack:** Java 21 / Spring Boot 3.2.4 / Spring Security / dev.samstevens.totp 1.7.1 / Flyway / Redis / Spring Mail (SMTP Gmail); Next.js 14 App Router / TypeScript strict / Tailwind tokens-only / React Query / Zustand / Vitest; gateway Spring Cloud Gateway.

**Spec:** `docs/superpowers/specs/2026-08-30-auth-completion-design.md`

## Global Constraints

- **Clean Architecture (auth-service):** domain/NEVER import Spring/JPA; application imports domain only; presentation → application only. Use-case-per-feature (không god-class service). Constructor injection (cấm `@Autowired` field; `@RequiredArgsConstructor` lên UseCase — test dùng constructor thật qua `new`, không `@InjectMocks` ngoài file test hiện có).
- **Naming/test:** `should_[ExpectedBehavior]_when_[Condition]`; 2-space indent; 100-col limit; package `com.spotify.auth.*`.
- **Token-only styling FE:** cấm arbitrary-hex (`text-[#…]`/`bg-[#…]`); dùng đúng classes hiện có trong login/register: `text-foreground`, `text-muted-foreground`, `bg-background`, `border-border`, `bg-spotify-green`, `border-muted`, `decoration-border`.
- **Server/Client:** default Server Component; thêm `"use client"` khi cần state/hooks.
- **Zustand granular selectors** — cấm destructure cả store.
- **Maven:** chạy từ `backend/`, dùng `/d/_mvn_tool/apache-maven-3.9.12/bin/mvn` + `-pl auth-service -am` (để common-lib build cùng reactor). Nếu `./mvnw` báo ClassNotFoundException → dùng `mvnw.cmd`.
- **FE test:** `npx vitest run <relative-path>` (vitest 4.1.11 không nhận `--` filter path). Mock api-client bằng `vi.mock("@/lib/api-client", () => ({ api: { get: vi.fn(), post: vi.fn(), patch: vi.fn() }, unwrap: (e) => e.data }))`.
- **TOTP spec (ADR D1–D3):** enroll lưu secret + `totpEnabled=false`; verify-setup code đúng → `totpEnabled=true`; login 2 bước khi `totpEnabled`; mfaToken Redis `MFA_CHALLENGE` TTL 5 phút, single-use (xóa khi code đúng, GIỮ khi sai cho retry).
- **Brute-force 2FA:** không thêm rate-limiter mới trong plan (ngoài scope — spec §7 ghi chú). MfaToken single-use + TTL 5 phút + `totpSecret==null` guard khi disable đã giới hạn số thử.
- Avatar URL-text only; 2FA local-only (OAuth2 Google giữ 1 bước); email SMTP Gmail thật (mock `EmailPort` trong test).
- `SecurityAuditPublisher.EventType` **đã có sẵn** `TWO_FA_ENABLED`, `TWO_FA_DISABLED`, `LOGIN_SUCCESS`, `PASSWORD_RESET`… — dùng, không thêm enum mới.

---

## Phase A — Backend hạ tầng TOTP + Migration

### Task 1: Flyway V2 migration + User domain TOTP fields

**Files:**
- Create: `backend/auth-service/src/main/resources/db/migration/V2__add_2fa_and_profile.sql`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/domain/entity/User.java`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/infrastructure/persistence/user/UserJpaMapper.java`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/infrastructure/persistence/user/UserJpaEntity.java`
- Create: `backend/auth-service/src/test/java/com/spotify/auth/domain/entity/UserTest.java`

**Interfaces:**
- Produces: `User` có fields `String totpSecret`, `boolean totpEnabled` (lombok `@Builder.Default false`), methods `isTwoFactorEnabled()` (tự viết — lombok tự sinh `isTotpEnabled()` từ field, không dùng), `storePendingTotpSecret(String)`, `enable2fa(String)`, `disable2fa()`.

- [ ] **Step 1: Write the failing test** `UserTest.java`

```java
package com.spotify.auth.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void should_BeDisabledByDefault_when_NewUser() {
    User user = User.builder().build();
    assertFalse(user.isTwoFactorEnabled());
  }

  @Test
  void should_StoreSecretWithoutEnabling_when_PendingEnrollment() {
    User user = User.builder().build();
    user.storePendingTotpSecret("JBSWY3DPEHPK3PXP");
    assertEquals("JBSWY3DPEHPK3PXP", user.getTotpSecret());
    assertFalse(user.isTwoFactorEnabled());
  }

  @Test
  void should_Enable2fa_When_ValidSecretProvided() {
    User user = User.builder().build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    assertTrue(user.isTwoFactorEnabled());
    assertEquals("JBSWY3DPEHPK3PXP", user.getTotpSecret());
  }

  @Test
  void should_Disable2fa_When_TurnedOff() {
    User user = User.builder().build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    user.disable2fa();
    assertFalse(user.isTwoFactorEnabled());
    assertNull(user.getTotpSecret());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=UserTest`
Expected: FAIL — compile error "cannot find symbol: isTwoFactorEnabled"

- [ ] **Step 3: Migration SQL** `V2__add_2fa_and_profile.sql`

```sql
-- TOTP 2FA (local accounts) + profile update support
ALTER TABLE users
    ADD COLUMN totp_secret   VARCHAR(255),
    ADD COLUMN totp_enabled  BOOLEAN NOT NULL DEFAULT FALSE;
```

- [ ] **Step 4: Add fields + behavior to `User.java`**

Thêm 2 field (trước `private OffsetDateTime createdAt;`, giữ nguyên `@Builder.Default` của `roles`/`isVerified`):
```java
    // === TOTP 2FA ===
    private String totpSecret;
    @Builder.Default
    private boolean totpEnabled = false;
```
Thêm methods (sau `updateProfile`):
```java
    public boolean isTwoFactorEnabled() {
        return this.totpEnabled;
    }

    /** Lưu secret 2FA khi enroll — CHƯA bật (bật sau khi verify code, ADR D2) */
    public void storePendingTotpSecret(String secret) {
        this.totpSecret = secret;
        this.updatedAt = OffsetDateTime.now();
    }

    public void enable2fa(String secret) {
        this.totpSecret = secret;
        this.totpEnabled = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public void disable2fa() {
        this.totpSecret = null;
        this.totpEnabled = false;
        this.updatedAt = OffsetDateTime.now();
    }
```

- [ ] **Step 5: Map field trong JPA persistence**

`UserJpaEntity.java` (import `jakarta.persistence.Column` nếu chưa có) thêm:
```java
    private String totpSecret;
    @Column(nullable = false)
    private boolean totpEnabled = false;
```
`UserJpaMapper.java` — `toEntity` thêm `.totpSecret(user.getTotpSecret()).totpEnabled(user.isTwoFactorEnabled())`; `toDomain` thêm `.totpSecret(entity.getTotpSecret()).totpEnabled(entity.isTotpEnabled())`.

- [ ] **Step 6: Run test to verify it passes**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=UserTest`
Expected: PASS 4/4

- [ ] **Step 7: Commit**

```bash
git add backend/auth-service/src/main/resources/db/migration/V2__add_2fa_and_profile.sql backend/auth-service/src/main/java/com/spotify/auth/domain/entity/User.java backend/auth-service/src/main/java/com/spotify/auth/infrastructure/persistence/user/UserJpaEntity.java backend/auth-service/src/main/java/com/spotify/auth/infrastructure/persistence/user/UserJpaMapper.java backend/auth-service/src/test/java/com/spotify/auth/domain/entity/UserTest.java
git commit -m "feat(auth): add TOTP secret fields + V2 migration"
```

---

### Task 2: TotpPort + TotpAdapter (đóng gói beans TotpConfig đã có)

**Files:**
- Create: `backend/auth-service/src/main/java/com/spotify/auth/application/port/out/TotpPort.java`
- Create: `backend/auth-service/src/main/java/com/spotify/auth/infrastructure/security/TotpAdapter.java`
- Create: `backend/auth-service/src/test/java/com/spotify/auth/infrastructure/security/TotpAdapterTest.java`

**Interfaces:**
- Produces: `interface TotpPort { String generateSecret(); String buildOtpAuthUri(String account, String issuer, String secret); String generateQrDataUri(String otpauthUri); boolean isValid(String code, String secret); }`
- Constructor `TotpAdapter(SecretGenerator, CodeVerifier, QrGenerator)` (beans `TotpConfig` public methods trả `DefaultSecretGenerator`/`DefaultCodeVerifier`/`ZxingPngQrGenerator`).

- [ ] **Step 1: Write the failing test** `TotpAdapterTest.java`

```java
package com.spotify.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import com.spotify.auth.infrastructure.config.TotpConfig;
import org.junit.jupiter.api.Test;

class TotpAdapterTest {

  private final TotpAdapter adapter = new TotpAdapter(
      new TotpConfig().secretGenerator(),
      new TotpConfig().codeVerifier(),
      new TotpConfig().qrGenerator());

  @Test
  void should_ReturnBase32Secret_when_Generating() {
    String secret = adapter.generateSecret();
    assertTrue(secret.matches("^[A-Z2-7]{16,32}$"));
  }

  @Test
  void should_BuildOtpAuthUri_when_ValidAccount() {
    String uri = adapter.buildOtpAuthUri("user@example.com", "Spotify Clone", "JBSWY3DPEHPK3PXP");
    assertTrue(uri.startsWith("otpauth://totp/"));
    assertTrue(uri.contains("issuer=Spotify%20Clone"));
    assertTrue(uri.contains("secret=JBSWY3DPEHPK3PXP"));
  }

  @Test
  void should_GenerateQrDataUri_when_ValidUri() {
    String qr = adapter.generateQrDataUri(
        "otpauth://totp/Spotify%20Clone:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Spotify%20Clone");
    assertTrue(qr.startsWith("data:image/png;base64,"));
  }

  @Test
  void should_RejectInvalidCode_when_CodeDoesNotMatchSecret() {
    // Xác suất 6 chữ số trùng TOTP hiện tại = 1/1.000.000 per 30s — chấp nhận được cho test
    assertFalse(adapter.isValid("000000", adapter.generateSecret()));
  }
}
```

- [ ] **Step 2: Run — verify fail**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=TotpAdapterTest`
Expected: FAIL — "cannot find symbol: class TotpAdapter"

- [ ] **Step 3: Write `TotpPort` interface**

```java
package com.spotify.auth.application.port.out;

/** Port cho TOTP 2FA — infrastructure adapter dùng dev.samstevens.totp. */
public interface TotpPort {

  String generateSecret();

  String buildOtpAuthUri(String account, String issuer, String secret);

  /** Base64 PNG data URI của QR code — FE <img src> render trực tiếp. */
  String generateQrDataUri(String otpauthUri);

  boolean isValid(String code, String secret);
}
```

- [ ] **Step 4: Write `TotpAdapter`**

```java
package com.spotify.auth.infrastructure.security;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;

import com.spotify.auth.application.port.out.TotpPort;

import java.util.Base64;

/** Adapter TOTP — wrap SecretGenerator/CodeVerifier/QrGenerator (beans TotpConfig). */
@RequiredArgsConstructor
public class TotpAdapter implements TotpPort {

  private final SecretGenerator secretGenerator;
  private final CodeVerifier codeVerifier;
  private final QrGenerator qrGenerator;

  @Override
  public String generateSecret() {
    return secretGenerator.generate();
  }

  @Override
  public String buildOtpAuthUri(String account, String issuer, String secret) {
    return new QrData.Builder()
        .issuer(issuer)
        .label(issuer + ":" + account)
        .secret(secret)
        .digits(6)
        .period(30)
        .build()
        .getUri();
  }

  @Override
  public String generateQrDataUri(String otpauthUri) {
    try {
      byte[] bytes = qrGenerator.generate(QrData.fromUri(otpauthUri), 200);
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    } catch (Exception e) {
      throw new IllegalStateException("Could not generate QR code", e);
    }
  }

  @Override
  public boolean isValid(String code, String secret) {
    return codeVerifier.isValidCode(secret, code);
  }
}
```
> **⚠️ CORRECTED AT IMPLEMENTATION (Ruling 2026-08-30):** `QrData.fromUri(String)` và `QrGenerator.generate(QrData, int)` KHÔNG tồn tại trong totp **1.7.1** (`javap` trên jar 1.7.1: `QrData` chỉ có `Builder` + package-private ctor; `QrGenerator.generate(QrData)` 1 arg; image size ở `ZxingPngQrGenerator.setImageSize`, default 200). Triển khai thực tế: `generateQrDataUri(String otpauthUri)` parse URI → rebuild `QrData` qua `QrData.Builder` (label từ path, issuer/secret/digits/period từ query, defaults digits=6 period=30) → `qrGenerator.generate(qrData)` (default size 200 = tương đương tham số 200 của plan). Port contract + test KHÔNG đổi. Quyết định này lưu tại ledger (`Ruling: QrData.fromUri`).

- [ ] **Step 5: Run — verify pass**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=TotpAdapterTest`
Expected: PASS 4/4 (nếu `isValid("000000", secret)` false-trùng xác suất 1/1e6 — chạy lại là được)

- [ ] **Step 6: Commit**

```bash
git add backend/auth-service/src/main/java/com/spotify/auth/application/port/out/TotpPort.java backend/auth-service/src/main/java/com/spotify/auth/infrastructure/security/TotpAdapter.java backend/auth-service/src/test/java/com/spotify/auth/infrastructure/security/TotpAdapterTest.java
git commit -m "feat(auth): add TotpPort adapter using existing TOTP beans"
```

---

## Phase B — Backend use cases TOTP + endpoints

### Task 3: EnrollTwoFactorUseCase + VerifyTwoFactorSetupUseCase + endpoints

**Files:**
- Create: `backend/auth-service/src/main/java/com/spotify/auth/application/usecase/EnrollTwoFactorUseCase.java`
- Create: `backend/auth-service/src/main/java/com/spotify/auth/application/usecase/VerifyTwoFactorSetupUseCase.java`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/presentation/controller/AuthController.java`
- Create: `backend/auth-service/src/test/java/com/spotify/auth/application/usecase/EnrollTwoFactorUseCaseTest.java`
- Create: `backend/auth-service/src/test/java/com/spotify/auth/application/usecase/VerifyTwoFactorSetupUseCaseTest.java`

**Consumes (đã verify tồn tại):** `UserRepository.findById(UUID)`/`save(User)`; `SecurityAuditPublisher.publish(userId, email, EventType, ip, ua, detail)` với `EventType.TWO_FA_ENABLED` (đã có); `TotpPort` (Task 2).
**Produces:** `EnrollTwoFactorUseCase.execute(UUID)` → `record EnrollResponse(String otpauthUrl, String qrDataUri)` (constructor 2-arg `(UserRepository, TotpPort)`, issuer hằng số `private static final String ISSUER = "Spotify Clone"`); `VerifyTwoFactorSetupUseCase.execute(UUID, String code)` (constructor 3-arg `(UserRepository, TotpPort, SecurityAuditPublisher)`); `VerifyTwoFactorSetupUseCase.Request(@NotBlank @Size(6) String code)` — tái dùng cho disable endpoint.

- [ ] **Step 1: Write failing tests**

`EnrollTwoFactorUseCaseTest.java`:
```java
package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnrollTwoFactorUseCaseTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final TotpPort totpPort = mock(TotpPort.class);
  private final EnrollTwoFactorUseCase useCase = new EnrollTwoFactorUseCase(userRepository, totpPort);

  @Test
  void should_ReturnOtpAuthAndQr_when_UserExists() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com")).build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(totpPort.generateSecret()).thenReturn("JBSWY3DPEHPK3PXP");
    when(totpPort.buildOtpAuthUri("user@example.com", "Spotify Clone", "JBSWY3DPEHPK3PXP"))
        .thenReturn("otpauth://totp/test");
    when(totpPort.generateQrDataUri("otpauth://totp/test")).thenReturn("data:image/png;base64,x");

    var response = useCase.execute(id);

    assertEquals("otpauth://totp/test", response.otpauthUrl());
    assertEquals("data:image/png;base64,x", response.qrDataUri());
    // ADR D2: secret lưu ngay, CHƯA bật — bật khi verify code
    assertFalse(user.isTwoFactorEnabled());
    assertEquals("JBSWY3DPEHPK3PXP", user.getTotpSecret());
    verify(userRepository).save(user);
  }

  @Test
  void should_ThrowException_when_UserNotFound() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());
    assertThrows(DomainException.class, () -> useCase.execute(id));
  }

  @Test
  void should_ThrowException_when_AlreadyEnabled() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com")).build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    assertThrows(DomainException.class, () -> useCase.execute(id));
  }
}
```

`VerifyTwoFactorSetupUseCaseTest.java`:
```java
package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerifyTwoFactorSetupUseCaseTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final TotpPort totpPort = mock(TotpPort.class);
  private final SecurityAuditPublisher auditPublisher = mock(SecurityAuditPublisher.class);
  private final VerifyTwoFactorSetupUseCase useCase =
      new VerifyTwoFactorSetupUseCase(userRepository, totpPort, auditPublisher);

  @Test
  void should_Enable2fa_when_CodeMatches() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com"))
        .totpSecret("JBSWY3DPEHPK3PXP").build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(totpPort.isValid("123456", "JBSWY3DPEHPK3PXP")).thenReturn(true);

    useCase.execute(id, "123456");

    assertTrue(user.isTwoFactorEnabled());
    verify(userRepository).save(user);
    verify(auditPublisher).publish(id.toString(), "user@example.com",
        SecurityAuditPublisher.EventType.TWO_FA_ENABLED, null, null, null);
  }

  @Test
  void should_Throw_when_CodeMismatch() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com"))
        .totpSecret("JBSWY3DPEHPK3PXP").build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(totpPort.isValid("000000", "JBSWY3DPEHPK3PXP")).thenReturn(false);

    assertThrows(DomainException.class, () -> useCase.execute(id, "000000"));
    assertFalse(user.isTwoFactorEnabled());
    verify(userRepository, never()).save(any());
  }

  @Test
  void should_Throw_when_NoSecretYet() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com")).build(); // totpSecret null
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    assertThrows(DomainException.class, () -> useCase.execute(id, "123456"));
    verify(userRepository, never()).save(any());
  }
}
```

- [ ] **Step 2: Run — verify fail**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=EnrollTwoFactorUseCaseTest,VerifyTwoFactorSetupUseCaseTest`
Expected: FAIL compile

- [ ] **Step 3: Write `EnrollTwoFactorUseCase`** (issuer hằng số — tránh `@Value` phá constructor/arity)

```java
package com.spotify.auth.application.usecase;

import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** UseCase: Bắt đầu đăng ký 2FA — sinh secret + lưu (CHƯA bật), trả QR cho FE. */
@Service
@RequiredArgsConstructor
public class EnrollTwoFactorUseCase {

  private static final String ISSUER = "Spotify Clone";

  public record EnrollResponse(String otpauthUrl, String qrDataUri) {}

  private final UserRepository userRepository;
  private final TotpPort totpPort;

  @Transactional
  public EnrollResponse execute(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException("User not found"));
    if (user.isTwoFactorEnabled()) {
      throw new DomainException("2FA is already enabled");
    }
    // ADR D2: lưu secret ngay để reload không mất QR, bật totpEnabled CHỈ khi verify code
    String secret = totpPort.generateSecret();
    String otpauthUri = totpPort.buildOtpAuthUri(user.getEmail().value(), ISSUER, secret);
    user.storePendingTotpSecret(secret);
    userRepository.save(user);
    return new EnrollResponse(otpauthUri, totpPort.generateQrDataUri(otpauthUri));
  }
}
```

- [ ] **Step 4: Write `VerifyTwoFactorSetupUseCase`**

```java
package com.spotify.auth.application.usecase;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** UseCase: Xác nhận mã TOTP khi setup → kích hoạt 2FA + audit. */
@Service
@RequiredArgsConstructor
public class VerifyTwoFactorSetupUseCase {

  public record Request(
      @NotBlank @Size(min = 6, max = 6, message = "Code must be 6 digits") String code) {}

  private final UserRepository userRepository;
  private final TotpPort totpPort;
  private final SecurityAuditPublisher auditPublisher;

  @Transactional
  public void execute(UUID userId, String code) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException("User not found"));
    if (user.getTotpSecret() == null) {
      throw new DomainException("No pending 2FA enrollment. Start enrollment first.");
    }
    if (!totpPort.isValid(code, user.getTotpSecret())) {
      throw new DomainException("Invalid 2FA code");
    }
    user.enable2fa(user.getTotpSecret()); // giữ secret + bật totpEnabled
    userRepository.save(user);
    auditPublisher.publish(userId.toString(), user.getEmail().value(),
        SecurityAuditPublisher.EventType.TWO_FA_ENABLED, null, null, null);
  }
}
```

- [ ] **Step 5: Add endpoints trong `AuthController`** + helper `requiredUserId`

Thêm 2 field constructor (lombok `@RequiredArgsConstructor` tự sinh) vào lớp controller (package `presentation/controller`), + imports `EnrollTwoFactorUseCase`, `VerifyTwoFactorSetupUseCase`. Cuối class:
```java
    // ===== TOTP 2FA =====

    @PostMapping("/2fa/enroll")
    public ResponseEntity<EnrollTwoFactorUseCase.EnrollResponse> enroll2fa(HttpServletRequest request) {
        UUID userId = requiredUserId(request);
        return ResponseEntity.ok(enrollTwoFactorUseCase.execute(userId));
    }

    @PostMapping("/2fa/verify")
    @ResponseStatus(HttpStatus.OK)
    public void verify2faSetup(@Valid @RequestBody VerifyTwoFactorSetupUseCase.Request request,
                               HttpServletRequest httpRequest) {
        verifyTwoFactorSetupUseCase.execute(requiredUserId(httpRequest), request.code());
    }
```
Helper (cạnh `getClientIp`):
```java
    private UUID requiredUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new com.spotify.auth.domain.exception.DomainException("Unauthorized");
        }
        return UUID.fromString(userIdStr);
    }
```
> `@SecurityRequirements()` cần cho các endpoint này (docs) nếu các method public khác trong controller có dùng — giữ nhất quán với `GET /me` hiện tại. Enroll/verify là endpoint authenticated (JWT) — đi qua gateway `authFilter`, không vào permitAll.

- [ ] **Step 6: Run — verify pass**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=EnrollTwoFactorUseCaseTest,VerifyTwoFactorSetupUseCaseTest`
Expected: PASS 3/3 + 3/3

- [ ] **Step 7: Commit**

```bash
git add backend/auth-service/src/main/java/com/spotify/auth/application/usecase/EnrollTwoFactorUseCase.java backend/auth-service/src/main/java/com/spotify/auth/application/usecase/VerifyTwoFactorSetupUseCase.java backend/auth-service/src/main/java/com/spotify/auth/presentation/controller/AuthController.java backend/auth-service/src/test/java/com/spotify/auth/application/usecase/EnrollTwoFactorUseCaseTest.java backend/auth-service/src/test/java/com/spotify/auth/application/usecase/VerifyTwoFactorSetupUseCaseTest.java
git commit -m "feat(auth): add 2FA enroll + verify-setup use cases and endpoints"
```

---

### Task 4: DisableTwoFactorUseCase + endpoint

**Files:**
- Create: `backend/auth-service/src/main/java/com/spotify/auth/application/usecase/DisableTwoFactorUseCase.java`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/presentation/controller/AuthController.java`
- Create: `backend/auth-service/src/test/java/com/spotify/auth/application/usecase/DisableTwoFactorUseCaseTest.java`

**Produces:** `DisableTwoFactorUseCase.execute(UUID userId, String code)` (constructor 3-arg `(UserRepository, TotpPort, SecurityAuditPublisher)`).

- [ ] **Step 1: Failing test** `DisableTwoFactorUseCaseTest.java`

```java
package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DisableTwoFactorUseCaseTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final TotpPort totpPort = mock(TotpPort.class);
  private final SecurityAuditPublisher auditPublisher = mock(SecurityAuditPublisher.class);
  private final DisableTwoFactorUseCase useCase =
      new DisableTwoFactorUseCase(userRepository, totpPort, auditPublisher);

  @Test
  void should_Disable_when_CodeMatches() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com"))
        .totpSecret("JBSWY3DPEHPK3PXP").build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(totpPort.isValid("123456", "JBSWY3DPEHPK3PXP")).thenReturn(true);

    useCase.execute(id, "123456");

    assertFalse(user.isTwoFactorEnabled());
    assertNull(user.getTotpSecret());
    verify(userRepository).save(user);
    verify(auditPublisher).publish(id.toString(), "user@example.com",
        SecurityAuditPublisher.EventType.TWO_FA_DISABLED, null, null, null);
  }

  @Test
  void should_Throw_when_CodeMismatch() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com"))
        .totpSecret("JBSWY3DPEHPK3PXP").build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(totpPort.isValid("000000", "JBSWY3DPEHPK3PXP")).thenReturn(false);

    assertThrows(DomainException.class, () -> useCase.execute(id, "000000"));
    assertTrue(user.isTwoFactorEnabled());
    verify(userRepository, never()).save(any());
  }
}
```

- [ ] **Step 2: Run — verify fail**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=DisableTwoFactorUseCaseTest`
Expected: FAIL

- [ ] **Step 3: Write `DisableTwoFactorUseCase`**

```java
package com.spotify.auth.application.usecase;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** UseCase: Tắt 2FA — verify code hiện tại trước khi xoá secret. */
@Service
@RequiredArgsConstructor
public class DisableTwoFactorUseCase {

  private final UserRepository userRepository;
  private final TotpPort totpPort;
  private final SecurityAuditPublisher auditPublisher;

  @Transactional
  public void execute(UUID userId, String code) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException("User not found"));
    if (user.getTotpSecret() == null) {
      throw new DomainException("2FA is not enabled");
    }
    if (!totpPort.isValid(code, user.getTotpSecret())) {
      throw new DomainException("Invalid 2FA code");
    }
    user.disable2fa(); // xoá secret + off
    userRepository.save(user);
    // MFA_CHALLENGE cũ còn hiệu lực (≤5 phút) không phải lo — verify-login guard tự chặn
    // (user.getTotpSecret()==null → "Invalid 2FA code"). SecurityTokenPort không có delete-by-user,
    // TTL 5 phút tự dọn — ghi chú này thay cho việc cố xoá theo user (spec §7).
    auditPublisher.publish(userId.toString(), user.getEmail().value(),
        SecurityAuditPublisher.EventType.TWO_FA_DISABLED, null, null, null);
  }
}
```

- [ ] **Step 4: Add endpoint**

`AuthController`:
```java
    @PostMapping("/2fa/disable")
    @ResponseStatus(HttpStatus.OK)
    public void disable2fa(@Valid @RequestBody VerifyTwoFactorSetupUseCase.Request request,
                           HttpServletRequest httpRequest) {
        disableTwoFactorUseCase.execute(requiredUserId(httpRequest), request.code());
    }
```
(dùng lại `Request` record của `VerifyTwoFactorSetupUseCase` — 6-digit code; thêm field constructor `DisableTwoFactorUseCase`.)

- [ ] **Step 5: Run — verify pass**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=DisableTwoFactorUseCaseTest`
Expected: PASS 2/2

- [ ] **Step 6: Commit**

```bash
git add backend/auth-service/src/main/java/com/spotify/auth/application/usecase/DisableTwoFactorUseCase.java backend/auth-service/src/main/java/com/spotify/auth/presentation/controller/AuthController.java backend/auth-service/src/test/java/com/spotify/auth/application/usecase/DisableTwoFactorUseCaseTest.java
git commit -m "feat(auth): add disable-2fa use case + endpoint"
```

---

### Task 5: LoginUseCase 2FA branch + VerifyTwoFactorLoginUseCase + endpoints

**Files:**
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/application/usecase/LoginUseCase.java`
- Create: `backend/auth-service/src/main/java/com/spotify/auth/application/usecase/VerifyTwoFactorLoginUseCase.java`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/presentation/controller/AuthController.java`
- Modify: `backend/auth-service/src/test/java/com/spotify/auth/application/usecase/LoginUseCaseTest.java`
- Create: `backend/auth-service/src/test/java/com/spotify/auth/application/usecase/VerifyTwoFactorLoginUseCaseTest.java`

**Consumes (đã verify):** `TokenPort.generateToken(User)`/`generateRefreshToken()`/`getAccessTokenExpirationMillis()`/`getRefreshTokenExpirationMillis()`; `RefreshToken.builder().token().userId().familyId().ipAddress().userAgent().expiresAt().createdAt().updatedAt().revoked()`; `SecurityTokenPort.save(token, userId, tokenType, ttlSeconds)`/`findUserIdByToken(token, type)` → `Optional<UUID>`/`delete(token, type)`; `SecurityAuditPublisher.publish(userId, email, EventType, ip, ua, detail)`.
**Produces:** `LoginUseCase.Response` thêm `boolean mfaRequired, String mfaToken, boolean twoFactorEnabled` (cuối record); `VerifyTwoFactorLoginUseCase` — trường hằng `TOKEN_TYPE = "MFA_CHALLENGE"`, `TTL_SECONDS = 300`, constructor 6-arg `(UserRepository, SecurityTokenPort, TotpPort, TokenPort, RefreshTokenRepository, SecurityAuditPublisher)`.

- [ ] **Step 1: Write failing test** `VerifyTwoFactorLoginUseCaseTest.java`

```java
package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.SecurityTokenPort;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerifyTwoFactorLoginUseCaseTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final SecurityTokenPort securityTokenPort = mock(SecurityTokenPort.class);
  private final TotpPort totpPort = mock(TotpPort.class);
  private final TokenPort tokenPort = mock(TokenPort.class);
  private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
  private final SecurityAuditPublisher auditPublisher = mock(SecurityAuditPublisher.class);
  private final VerifyTwoFactorLoginUseCase useCase =
      new VerifyTwoFactorLoginUseCase(userRepository, securityTokenPort, totpPort,
          tokenPort, refreshTokenRepository, auditPublisher);

  private User twoFaUser() {
    User user = User.builder()
        .id(UUID.randomUUID())
        .email(new Email("user@example.com"))
        .displayName("User")
        .totpSecret("JBSWY3DPEHPK3PXP")
        .build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    return user;
  }

  @Test
  void should_IssueTokens_when_MfaTokenAndCodeValid() {
    User user = twoFaUser();
    when(securityTokenPort.findUserIdByToken("mfa-1", VerifyTwoFactorLoginUseCase.TOKEN_TYPE))
        .thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(totpPort.isValid("123456", "JBSWY3DPEHPK3PXP")).thenReturn(true);
    when(tokenPort.generateToken(user)).thenReturn("access-token");
    when(tokenPort.generateRefreshToken()).thenReturn("refresh-token");
    when(tokenPort.getAccessTokenExpirationMillis()).thenReturn(900000L);
    when(tokenPort.getRefreshTokenExpirationMillis()).thenReturn(604800000L);
    when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    doNothing().when(auditPublisher).publish(any(), any(), any(), any(), any(), any());

    var response = useCase.execute(
        new VerifyTwoFactorLoginUseCase.Request("mfa-1", "123456", "127.0.0.1", "UA"));

    assertEquals(user.getId().toString(), response.userId());
    assertEquals("access-token", response.accessToken());
    verify(securityTokenPort).delete("mfa-1", VerifyTwoFactorLoginUseCase.TOKEN_TYPE);
    verify(refreshTokenRepository).save(any());
    verify(auditPublisher).publish(user.getId().toString(), "user@example.com",
        SecurityAuditPublisher.EventType.LOGIN_SUCCESS, "127.0.0.1", "UA", null);
  }

  @Test
  void should_Throw_when_MfaTokenInvalid() {
    when(securityTokenPort.findUserIdByToken("bad", VerifyTwoFactorLoginUseCase.TOKEN_TYPE))
        .thenReturn(Optional.empty());
    assertThrows(DomainException.class, () -> useCase.execute(
        new VerifyTwoFactorLoginUseCase.Request("bad", "123456", "127.0.0.1", "UA")));
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void should_Throw_when_CodeMismatch() {
    User user = twoFaUser();
    when(securityTokenPort.findUserIdByToken("mfa-1", VerifyTwoFactorLoginUseCase.TOKEN_TYPE))
        .thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(totpPort.isValid("000000", "JBSWY3DPEHPK3PXP")).thenReturn(false);

    assertThrows(DomainException.class, () -> useCase.execute(
        new VerifyTwoFactorLoginUseCase.Request("mfa-1", "000000", "127.0.0.1", "UA")));
    // Token KHÔNG bị xoá khi code sai — cho retry tới hết TTL 5 phút (ADR D3)
    verify(securityTokenPort, never()).delete(any(), any());
    verify(refreshTokenRepository, never()).save(any());
  }
}
```

- [ ] **Step 2: Run — verify fail**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=VerifyTwoFactorLoginUseCaseTest`
Expected: FAIL

- [ ] **Step 3: Write `VerifyTwoFactorLoginUseCase`** (đầy đủ — không placeholder)

```java
package com.spotify.auth.application.usecase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import com.spotify.auth.application.port.out.SecurityTokenPort;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.application.port.out.TotpPort;
import com.spotify.auth.domain.entity.RefreshToken;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.domain.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/** UseCase: Xác nhận mã TOTP khi login — mfaToken single-use (Redis 5 phút, ADR D1/D3). */
@Service
@RequiredArgsConstructor
public class VerifyTwoFactorLoginUseCase {

  public static final String TOKEN_TYPE = "MFA_CHALLENGE";
  public static final long TTL_SECONDS = 5 * 60L;

  public record Request(
      @NotBlank String mfaToken,
      @NotBlank @Size(min = 6, max = 6, message = "Code must be 6 digits") String code,
      @JsonIgnore String ipAddress,
      @JsonIgnore String userAgent) {}

  public record Response(
      @JsonIgnore String accessToken,
      @JsonIgnore String refreshToken,
      String userId,
      String email,
      String displayName,
      String avatarUrl,
      long expiresIn) {}

  private final UserRepository userRepository;
  private final SecurityTokenPort securityTokenPort;
  private final TotpPort totpPort;
  private final TokenPort tokenPort;
  private final RefreshTokenRepository refreshTokenRepository;
  private final SecurityAuditPublisher auditPublisher;

  @Transactional
  public Response execute(Request request) {
    UUID userId = securityTokenPort.findUserIdByToken(request.mfaToken(), TOKEN_TYPE)
        .orElseThrow(() -> new DomainException("2FA session expired. Please log in again."));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException("User not found"));
    if (user.getTotpSecret() == null
        || !user.isTwoFactorEnabled()
        || !totpPort.isValid(request.code(), user.getTotpSecret())) {
      throw new DomainException("Invalid 2FA code");
    }
    // Mã đúng → mfaToken single-use, xoá ngay. Mã sai → GIỮ token cho retry tới hết TTL.
    securityTokenPort.delete(request.mfaToken(), TOKEN_TYPE);

    String accessToken = tokenPort.generateToken(user);
    String refreshTokenStr = tokenPort.generateRefreshToken();
    RefreshToken rt = RefreshToken.builder()
        .token(refreshTokenStr)
        .userId(user.getId())
        .familyId(UUID.randomUUID())
        .ipAddress(request.ipAddress())
        .userAgent(request.userAgent())
        .expiresAt(OffsetDateTime.now().plus(Duration.ofMillis(tokenPort.getRefreshTokenExpirationMillis())))
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
    refreshTokenRepository.save(rt);

    // Đăng nhập 2FA thành công → LOGIN_SUCCESS audit (bước nhập mật khẩu KHÔNG audit — spec §7)
    auditPublisher.publish(user.getId().toString(), user.getEmail().value(),
        SecurityAuditPublisher.EventType.LOGIN_SUCCESS, request.ipAddress(), request.userAgent(), null);

    long expiresIn = tokenPort.getAccessTokenExpirationMillis() / 1000;
    return new Response(accessToken, refreshTokenStr, user.getId().toString(),
        user.getEmail().value(), user.getDisplayName(), user.getAvatarUrl(), expiresIn);
  }
}
```

- [ ] **Step 4: LoginUseCase 2FA branch** (ADR D3) — thay thế nguyên trạng thái login hiện tại

Sửa `LoginUseCase.java`:

1. Thêm imports: `com.spotify.auth.application.port.out.SecurityTokenPort`, `com.spotify.auth.application.port.out.TotpPort`.
2. Thêm 2 final fields (trong constructor hiện có):
```java
    private final SecurityTokenPort securityTokenPort;
    private final TotpPort totpPort;
```
3. Response record — thêm 3 field cuối:
```java
    public record Response(
            @JsonIgnore String accessToken,
            @JsonIgnore String refreshToken,
            String userId,
            String email,
            String displayName,
            String avatarUrl,
            long expiresIn,
            boolean mfaRequired,
            String mfaToken,
            boolean twoFactorEnabled
    ) {}
```
4. Trong `execute`, ngay sau `user.recordSuccessfulLogin(); userRepository.save(user);` chèn:
```java
        // 2FA bật (local account): KHÔNG cấp token/cookie — trả mfaToken challenge (ADR D3)
        if (user.isTwoFactorEnabled()) {
            String mfaToken = UUID.randomUUID().toString();
            securityTokenPort.save(mfaToken, user.getId(), VerifyTwoFactorLoginUseCase.TOKEN_TYPE,
                    VerifyTwoFactorLoginUseCase.TTL_SECONDS);
            return new Response(null, null, user.getId().toString(), user.getEmail().value(),
                    user.getDisplayName(), user.getAvatarUrl(), 0, true, mfaToken, true);
        }
```
5. Return cuối (login bình thường) thêm `, false, null, false`:
```java
        return new Response(accessToken, refreshTokenStr, user.getId().toString(),
                user.getEmail().value(), user.getDisplayName(), user.getAvatarUrl(), expiresIn,
                false, null, false);
```

- [ ] **Step 5: Update `LoginUseCaseTest`** — thêm 2 `@Mock` + test mới

Thêm imports: `com.spotify.auth.application.port.out.SecurityTokenPort`, `com.spotify.auth.application.port.out.TotpPort`, `static org.mockito.ArgumentMatchers.eq`, `static org.mockito.ArgumentMatchers.anyLong` (nếu chưa có). Thêm @Mock fields:
```java
    @Mock
    private SecurityTokenPort securityTokenPort;

    @Mock
    private TotpPort totpPort;
```
Sửa dòng init: nếu test hiện dùng `@InjectMocks LoginUseCase loginUseCase;` — giữ nguyên; Spring constructor sẽ nhận thứ tự fields. Các stub test cũ không chạm 2 port mới — an toàn.

Thêm test mới (cuối class):
```java
    @Test
    void should_ReturnMfaRequired_when_UserHas2faEnabled() {
        // Given
        LoginUseCase.Request request = new LoginUseCase.Request("test@example.com", "Test1234", "127.0.0.1", "JUnit");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(new Email("test@example.com"))
                .password(new Password("Hashed1234"))
                .displayName("User Name")
                .totpSecret("JBSWY3DPEHPK3PXP")
                .build();
        user.enable2fa("JBSWY3DPEHPK3PXP");

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("Test1234", "Hashed1234")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(securityTokenPort).save(any(String.class), any(UUID.class), any(String.class), anyLong());

        // When
        LoginUseCase.Response response = loginUseCase.execute(request);

        // Then
        assertTrue(response.mfaRequired());
        assertNotNull(response.mfaToken());
        assertTrue(response.twoFactorEnabled());
        assertNull(response.accessToken());
        // Không tạo refresh token + không audit LOGIN_SUCCESS ở bước nhập mật khẩu
        verify(refreshTokenRepository, never()).save(any());
        verify(auditPublisher, never()).publish(any(), any(), any(), any(), any(), any());
        verify(securityTokenPort).save(any(), eq(user.getId()), eq("MFA_CHALLENGE"), eq(300L));
    }
```
> Test cũ `should_LoginSuccessfully_when_CredentialsAreValid` không cần sửa — 2FA off → branch bị bỏ qua, 2 port mới không được gọi.

- [ ] **Step 6: Add endpoint + guard cookie khi mfaRequired**

`AuthController`:
```java
    @PostMapping("/2fa/verify-login")
    @SecurityRequirements()
    public ResponseEntity<VerifyTwoFactorLoginUseCase.Response> verify2faLogin(
            @Valid @RequestBody VerifyTwoFactorLoginUseCase.Request request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String ip = getClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        var result = verifyTwoFactorLoginUseCase.execute(
            new VerifyTwoFactorLoginUseCase.Request(request.mfaToken(), request.code(), ip, ua));
        setAuthCookies(response, result.accessToken(), result.refreshToken());
        return ResponseEntity.ok(result);
    }
```
**QUAN TRỌNG — sửa login endpoint hiện tại** (tránh NPE khi accessToken/refreshToken là null → `ResponseCookie.from(name, null)` fail):
```java
        LoginUseCase.Response result = loginUseCase.execute(new LoginUseCase.Request(
                request.email(), request.password(), ip, userAgent));

        if (!result.mfaRequired()) {
            setAuthCookies(response, result.accessToken(), result.refreshToken());
        }
        return ResponseEntity.ok(result);
```

- [ ] **Step 7: Run full auth-service test gate**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am`
Expected: PASS — toàn bộ auth-service (13 test cũ + mới) xanh

- [ ] **Step 8: Commit**

```bash
git add backend/auth-service/src/main/java/com/spotify/auth/application/usecase/LoginUseCase.java backend/auth-service/src/main/java/com/spotify/auth/application/usecase/VerifyTwoFactorLoginUseCase.java backend/auth-service/src/main/java/com/spotify/auth/presentation/controller/AuthController.java backend/auth-service/src/test/java/com/spotify/auth/application/usecase/LoginUseCaseTest.java backend/auth-service/src/test/java/com/spotify/auth/application/usecase/VerifyTwoFactorLoginUseCaseTest.java
git commit -m "feat(auth): 2-step login with TOTP mfa challenge"
```

---

### Task 6: UpdateProfileUseCase + PATCH /me + GetCurrentUserUseCase bổ sung trạng thái

**Files:**
- Create: `backend/auth-service/src/main/java/com/spotify/auth/application/usecase/UpdateProfileUseCase.java`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/application/usecase/GetCurrentUserUseCase.java`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/presentation/controller/AuthController.java`
- Create: `backend/auth-service/src/test/java/com/spotify/auth/application/usecase/UpdateProfileUseCaseTest.java`
- Create: `backend/auth-service/src/test/java/com/spotify/auth/application/usecase/GetCurrentUserUseCaseTest.java`

**Consumes:** `GetCurrentUserUseCase.execute(UUID)` → `Response(boolean, UserResponse)`; `User.updateProfile(displayName, avatarUrl)` (đã có); `User.isVerified()`.
**Produces:** `GetCurrentUserUseCase.UserResponse` = `(String id, String email, String displayName, String avatarUrl, boolean emailVerified, boolean twoFactorEnabled)` — thêm 2 field cuối, `Response(boolean success, UserResponse data)` giữ nguyên. `UpdateProfileUseCase.execute(UUID, Request)` → `UserResponse`; `Request(displayName?, avatarUrl?)` record.

- [ ] **Step 1: Failing tests**

`UpdateProfileUseCaseTest.java`:
```java
package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateProfileUseCaseTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final UpdateProfileUseCase useCase = new UpdateProfileUseCase(userRepository);

  @Test
  void should_UpdateDisplayName_when_Provided() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("user@example.com"))
        .displayName("Old").avatarUrl(null).build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    var response = useCase.execute(id, new UpdateProfileUseCase.Request("New Name", null));

    assertEquals("New Name", response.displayName());
    assertNull(response.avatarUrl());
    assertFalse(response.twoFactorEnabled());
    verify(userRepository).save(user);
  }

  @Test
  void should_KeepDisplayName_when_OnlyAvatarProvided() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id).email(new Email("u@e.com"))
        .displayName("Keep").avatarUrl(null).build();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    var response = useCase.execute(id, new UpdateProfileUseCase.Request(null, "https://i.img/a.png"));

    assertEquals("Keep", response.displayName());
    assertEquals("https://i.img/a.png", response.avatarUrl());
  }

  @Test
  void should_Throw_when_UserNotFound() {
    when(userRepository.findById(any())).thenReturn(Optional.empty());
    assertThrows(DomainException.class,
        () -> useCase.execute(UUID.randomUUID(), new UpdateProfileUseCase.Request("X", "url")));
  }

  @Test
  void should_Throw_when_BothFieldsNull() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.of(User.builder().id(id).build()));
    assertThrows(DomainException.class,
        () -> useCase.execute(id, new UpdateProfileUseCase.Request(null, null)));
  }
}
```

`GetCurrentUserUseCaseTest.java`:
```java
package com.spotify.auth.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetCurrentUserUseCaseTest {

  private final UserRepository userRepository = Mockito.mock(UserRepository.class);
  private final GetCurrentUserUseCase useCase = new GetCurrentUserUseCase(userRepository);

  @Test
  void should_ReturnProfileWithStateFlags_when_UserExists() {
    UUID id = UUID.randomUUID();
    User user = User.builder().id(id)
        .email(new Email("u@e.com"))
        .displayName("U")
        .totpSecret("SEC")
        .build();
    user.verifyEmail();
    user.enable2fa("SEC");
    Mockito.when(userRepository.findById(id)).thenReturn(Optional.of(user));

    var response = useCase.execute(id);

    assertTrue(response.success());
    assertEquals(true, response.data().emailVerified());
    assertEquals(true, response.data().twoFactorEnabled());
  }
}
```
> Bắt buộc cần `user.verifyEmail()` — kiểm tra tên method thật trong `User` trước khi viết (đã có trong spec: `isVerified` field + method verify). Nếu method khác tên, dùng đúng method hiện có.

- [ ] **Step 2: Run — verify fail**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=UpdateProfileUseCaseTest,GetCurrentUserUseCaseTest`
Expected: FAIL

- [ ] **Step 3: Write `UpdateProfileUseCase`**

```java
package com.spotify.auth.application.usecase;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.exception.DomainException;
import com.spotify.auth.domain.repository.UserRepository;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** UseCase: Cập nhật profile (displayName/avatarUrl) — cả 2 optional, ít nhất 1 non-null (PATCH phần). */
@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase {

  public record Request(
      @Size(max = 255) String displayName,
      @Size(max = 255) String avatarUrl) {}

  private final UserRepository userRepository;

  @Transactional
  public GetCurrentUserUseCase.UserResponse execute(UUID userId, Request request) {
    if (request.displayName() == null && request.avatarUrl() == null) {
      throw new DomainException("At least one field (displayName or avatarUrl) is required");
    }
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException("User not found"));

    String newName = request.displayName() != null ? request.displayName().trim() : user.getDisplayName();
    String newAvatar = request.avatarUrl() != null ? request.avatarUrl() : user.getAvatarUrl();
    user.updateProfile(newName, newAvatar);
    userRepository.save(user);

    return new GetCurrentUserUseCase.UserResponse(
        user.getId().toString(), user.getEmail().value(), user.getDisplayName(),
        user.getAvatarUrl(), user.isVerified(), user.isTwoFactorEnabled());
  }
}
```

- [ ] **Step 4: Modify `GetCurrentUserUseCase`** — UserResponse + 2 field

Record mới (thay record cũ):
```java
    public record UserResponse(String id, String email, String displayName, String avatarUrl,
                               boolean emailVerified, boolean twoFactorEnabled) {}
```
Map trong `execute`: sau `user.getAvatarUrl()` thêm `, user.isVerified(), user.isTwoFactorEnabled()`.

- [ ] **Step 5: Add PATCH endpoint**

`AuthController` (import `org.springframework.web.bind.annotation.PatchMapping`):
```java
    @PatchMapping("/me")
    public ResponseEntity<GetCurrentUserUseCase.UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileUseCase.Request request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(updateProfileUseCase.execute(requiredUserId(httpRequest), request));
    }
```
Thêm field constructor `UpdateProfileUseCase`. Verify controller CORS `allowedMethods` có PATCH (Task 8 bổ sung — nếu chưa, thêm sớm ở task này: `configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"))` trong `SecurityConfig`).

- [ ] **Step 6: Run — verify pass**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=UpdateProfileUseCaseTest,GetCurrentUserUseCaseTest`
Expected: PASS 5/5

- [ ] **Step 7: Commit**

```bash
git add backend/auth-service/src/main/java/com/spotify/auth/application/usecase/UpdateProfileUseCase.java backend/auth-service/src/main/java/com/spotify/auth/application/usecase/GetCurrentUserUseCase.java backend/auth-service/src/main/java/com/spotify/auth/presentation/controller/AuthController.java backend/auth-service/src/test/java/com/spotify/auth/application/usecase/UpdateProfileUseCaseTest.java backend/auth-service/src/test/java/com/spotify/auth/application/usecase/GetCurrentUserUseCaseTest.java
git commit -m "feat(auth): add PATCH /me profile update + enrich /me response"
```

---

### Task 7: RegisterUseCase auto-send verification email

**Files:**
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/application/usecase/RegisterUseCase.java`
- Modify: `backend/auth-service/src/test/java/com/spotify/auth/application/usecase/RegisterUseCaseTest.java`

**Consumes:** `SecurityTokenPort.save`; `EmailPort.sendVerificationEmail(email, displayName, link)`; `@Value("${app.base-url:http://localhost:3000}")`.
**Produces:** RegisterUseCase thêm 2 final deps `SecurityTokenPort`, `EmailPort` + 1 `@Value` non-final field `baseUrl`. Sau khi publish `UserRegistered` → auto-send verification email (token `"EMAIL_VERIFICATION"`, TTL 24h, link `baseUrl + "/verify-email?token=" + token`).

- [ ] **Step 1: Modify RegisterUseCaseTest — thêm @Mock + test mới (đang FAIL)**

Thêm imports `com.spotify.auth.application.port.out.EmailPort`, `com.spotify.auth.application.port.out.SecurityTokenPort`, `static org.mockito.ArgumentMatchers.eq`, `static org.mockito.ArgumentMatchers.contains`.
Thêm fields:
```java
    @Mock
    private SecurityTokenPort securityTokenPort;

    @Mock
    private EmailPort emailPort;
```
Thêm test mới:
```java
    @Test
    void should_SendVerificationEmail_when_RegisterSucceeds() {
        // Given
        RegisterUseCase.Request request = new RegisterUseCase.Request("test@example.com", "Test1234", "User Name", "avatar.url");
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoderPort.encode(anyString())).thenReturn("Hashed1234");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(UUID.randomUUID())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .displayName(user.getDisplayName())
                    .avatarUrl(user.getAvatarUrl())
                    .build();
        });
        when(tokenPort.generateToken(any(User.class))).thenReturn("fake-jwt-token");
        doNothing().when(securityTokenPort).save(any(), any(), any(), anyLong());
        doNothing().when(emailPort).sendVerificationEmail(anyString(), anyString(), anyString());

        // When — auto-send sau khi lưu user + publish event (spec D6)
        registerUseCase.execute(request);

        // Then
        verify(securityTokenPort).save(any(), any(), eq("EMAIL_VERIFICATION"), eq(24L * 60 * 60));
        verify(emailPort).sendVerificationEmail(eq("test@example.com"), eq("User Name"),
                contains("/verify-email?token="));
    }
```
> Các test cũ vẫn chạy: `securityTokenPort`/`emailPort` là mocks void → không cần stub. Nếu test cũ dùng `@InjectMocks` + constructor cũ 5-arg, thêm 2 arg mới tự động theo field order.

- [ ] **Step 2: Run — verify fail (constructor arity)**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=RegisterUseCaseTest`
Expected: FAIL

- [ ] **Step 3: Modify `RegisterUseCase`** — thêm deps + auto-send

Thêm imports `com.spotify.auth.application.port.out.EmailPort`, `com.spotify.auth.application.port.out.SecurityTokenPort`. Thêm final fields (cuối danh sách constructor):
```java
    private final SecurityTokenPort securityTokenPort;
    private final EmailPort emailPort;
```
Thêm `@Value` field (non-final, pattern giống `ForgotPasswordUseCase`):
```java
    @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;
```
Trong `execute`, ngay sau `domainEventPublisher.publish(new UserRegistered(...));` chèn:
```java
        // Auto-send verification email (spec D6) — token EMAIL_VERIFICATION TTL 24h, single-use
        String verificationToken = UUID.randomUUID().toString();
        securityTokenPort.save(verificationToken, user.getId(), "EMAIL_VERIFICATION", 24 * 60 * 60L);
        String verificationLink = baseUrl + "/verify-email?token=" + verificationToken;
        emailPort.sendVerificationEmail(user.getEmail().value(), user.getDisplayName(), verificationLink);
```

- [ ] **Step 4: Run — verify pass**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am -Dtest=RegisterUseCaseTest`
Expected: PASS 3/3

- [ ] **Step 5: Commit**

```bash
git add backend/auth-service/src/main/java/com/spotify/auth/application/usecase/RegisterUseCase.java backend/auth-service/src/test/java/com/spotify/auth/application/usecase/RegisterUseCaseTest.java
git commit -m "feat(auth): auto-send verification email on register"
```

---

## Phase C — Gateway + Security public routes + Cookie consistency

### Task 8: Gateway + auth-service SecurityConfig permit public auth routes

**Files:**
- Modify: `gateway/src/main/java/com/spotify/gateway/config/GatewayConfig.java`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/infrastructure/config/SecurityConfig.java`

**Rationale (đã verify):** `JwtAuthFilter` gateway 401 khi không có JWT. `forgot-password`, `reset-password`, `send-verification`, `verify-email`, `2fa/verify-login` được gọi khi **chưa có JWT/cookie** (quên pass trước login; verify từ email; 2FA login trước cookie). Hiện route bypass chỉ gồm login/register/refresh → các flow này 401 tại gateway.

- [ ] **Step 1: Modify `GatewayConfig`** — thêm 5 path vào cả bypass route và protected `not()`

```java
                .route("auth-service-login", r -> r.path(
                        "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh",
                        "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                        "/api/v1/auth/send-verification", "/api/v1/auth/verify-email",
                        "/api/v1/auth/2fa/verify-login")
                        .uri("http://localhost:8081")) // Forward to Auth Service without JWT check
                .route("auth-service-protected", r -> r.path("/api/v1/auth/**")
                        .and().not(p -> p.path(
                            "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh",
                            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                            "/api/v1/auth/send-verification", "/api/v1/auth/verify-email",
                            "/api/v1/auth/2fa/verify-login"))
                        .filters(f -> f.filter(authFilter))
                        .uri("http://localhost:8081"))
```
> PATCH /me + 2fa/enroll|verify|disable vẫn qua `authFilter` (có JWT) — không đổi.

- [ ] **Step 2: Modify `SecurityConfig.authSecurityFilterChain`** permitAll thêm 5 path

```java
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register",
                                "/api/v1/auth/refresh", "/error/**",
                                "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                                "/api/v1/auth/send-verification", "/api/v1/auth/verify-email",
                                "/api/v1/auth/2fa/verify-login").permitAll()
```
Và thêm `"PATCH"` vào `corsConfigurationSource`:
```java
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
```

- [ ] **Step 3: Verify gateway + auth-service build**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am`
Run: `cd gateway && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q compile`
Expected: cả 2 xanh

- [ ] **Step 4: Commit**

```bash
git add gateway/src/main/java/com/spotify/gateway/config/GatewayConfig.java backend/auth-service/src/main/java/com/spotify/auth/infrastructure/config/SecurityConfig.java
git commit -m "fix(gateway): permit public auth routes (forgot/reset/verify/2fa-login)"
```

---

### Task 9: Cookie consistency — shared AuthCookieFactory + OAuth2SuccessHandler đồng bộ

**Files:**
- Create: `backend/auth-service/src/main/java/com/spotify/auth/infrastructure/security/AuthCookieFactory.java`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/presentation/controller/AuthController.java`
- Modify: `backend/auth-service/src/main/java/com/spotify/auth/infrastructure/security/oauth2/OAuth2SuccessHandler.java`

**Rationale (ADR D5):** `OAuth2SuccessHandler` set refresh cookie `path=/api/v1/auth/refresh` + KHÔNG domain; `AuthController` set `path=/` + `domain=cookieDomain`. Lệch → sau OAuth2 login cookie không gửi cho request khác.
**Layering note:** factory đặt trong `infrastructure/security` (web-adapter glue). `AuthController` (presentation) import từ infrastructure ở biên web — concession layer nhỏ đã chốt ở spec D5 để DRY giữa 2 nơi gần HTTP.

- [ ] **Step 1: Create `AuthCookieFactory`**

```java
package com.spotify.auth.infrastructure.security;

import org.springframework.http.ResponseCookie;

/** Build HttpOnly auth cookies — dùng chung cho login/register/refresh/OAuth2 (ADR D5). */
public final class AuthCookieFactory {

  private AuthCookieFactory() {
  }

  public static ResponseCookie accessTokenCookie(String token, long maxAgeSeconds, String domain) {
    return cookie("auth-token", token, maxAgeSeconds, domain);
  }

  public static ResponseCookie refreshTokenCookie(String token, long maxAgeSeconds, String domain) {
    return cookie("refresh-token", token, maxAgeSeconds, domain);
  }

  public static ResponseCookie clearCookie(String name, String domain) {
    return cookie(name, "", 0, domain);
  }

  private static ResponseCookie cookie(String name, String value, long maxAgeSeconds, String domain) {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(false) // Set to true in production (HTTPS)
        .path("/")
        .domain(domain)
        .maxAge(maxAgeSeconds)
        .sameSite("Lax")
        .build();
  }
}
```

- [ ] **Step 2: Refactor `AuthController`** — thay body 2 private methods bằng factory

`setAuthCookies`:
```java
    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader("Set-Cookie",
            AuthCookieFactory.accessTokenCookie(accessToken, 15 * 60, cookieDomain).toString());
        response.addHeader("Set-Cookie",
            AuthCookieFactory.refreshTokenCookie(refreshToken, 7 * 24 * 60 * 60, cookieDomain).toString());
    }
```
`clearAuthCookies`:
```java
    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", AuthCookieFactory.clearCookie("auth-token", cookieDomain).toString());
        response.addHeader("Set-Cookie", AuthCookieFactory.clearCookie("refresh-token", cookieDomain).toString());
    }
```
Import `com.spotify.auth.infrastructure.security.AuthCookieFactory`; bỏ imports unused (`ResponseCookie` nếu không còn dùng).

- [ ] **Step 3: Modify `OAuth2SuccessHandler`** — dùng factory + `domain`

Thêm `@Value` non-final field:
```java
    @org.springframework.beans.factory.annotation.Value("${app.cookie-domain:localhost}")
    private String cookieDomain;
```
Thay block build cookie (đoạn "3. Set HttpOnly cookies") bằng:
```java
        // Đồng bộ với AuthController: path /, domain, sameSite Lax (ADR D5)
        response.addHeader("Set-Cookie", AuthCookieFactory.accessTokenCookie(
            accessToken, accessExpiresIn / 1000, cookieDomain).toString());
        response.addHeader("Set-Cookie", AuthCookieFactory.refreshTokenCookie(
            refreshTokenStr, refreshExpiresIn / 1000, cookieDomain).toString());
```
Import `com.spotify.auth.infrastructure.security.AuthCookieFactory`; bỏ unused `ResponseCookie` import nếu có.

- [ ] **Step 4: Verify build**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am`
Expected: PASS (auth-service test full)

- [ ] **Step 5: Commit**

```bash
git add backend/auth-service/src/main/java/com/spotify/auth/infrastructure/security/AuthCookieFactory.java backend/auth-service/src/main/java/com/spotify/auth/presentation/controller/AuthController.java backend/auth-service/src/main/java/com/spotify/auth/infrastructure/security/oauth2/OAuth2SuccessHandler.java
git commit -m "fix(auth): unify HttpOnly cookie building across login and OAuth2"
```

---

## Phase D — FE services + validation + hooks

### Task 10: FE types + AuthService mở rộng

**Files:**
- Modify: `frontend/types/auth.ts`
- Modify: `frontend/services/api/authService.ts`
- Create: `frontend/services/api/__tests__/authService.test.ts`

**Interfaces:**
- Produces: `ProfileResponse` thêm `emailVerified: boolean; twoFactorEnabled: boolean;`; `AuthResponse` thêm `mfaRequired?`, `mfaToken?`, `twoFactorEnabled?`, `emailVerified?`; thêm `UpdateProfileRequest`, `Enroll2faResponse`. `AuthService` methods mới: `forgotPassword`, `resetPassword`, `sendVerification`, `verifyEmail`, `updateProfile`, `enroll2fa`, `verify2faSetup`, `disable2fa`, `verify2faLogin`. `me()` trả `ApiResult<ProfileResponse>` (như hiện tại).

- [ ] **Step 1: Write failing test** `authService.test.ts`

```ts
import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("@/lib/api-client", () => ({
  api: { get: vi.fn(), post: vi.fn(), patch: vi.fn() },
  unwrap: (envelope: { data: unknown }) => envelope.data,
}));

import { api } from "@/lib/api-client";
import { AuthService } from "@/services/api/authService";

describe("AuthService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("forgotPassword posts email to /auth/forgot-password", async () => {
    vi.mocked(api.post).mockResolvedValue({ message: "ok" });
    await AuthService.forgotPassword("a@b.com");
    expect(api.post).toHaveBeenCalledWith("/auth/forgot-password", { email: "a@b.com" });
  });

  it("resetPassword posts token+newPassword", async () => {
    vi.mocked(api.post).mockResolvedValue({ message: "ok" });
    await AuthService.resetPassword("tok", "NewPass123");
    expect(api.post).toHaveBeenCalledWith("/auth/reset-password", {
      token: "tok",
      newPassword: "NewPass123",
    });
  });

  it("verifyEmail posts token", async () => {
    vi.mocked(api.post).mockResolvedValue({ message: "ok" });
    await AuthService.verifyEmail("vtok");
    expect(api.post).toHaveBeenCalledWith("/auth/verify-email", { token: "vtok" });
  });

  it("updateProfile patches /auth/me and returns enriched profile", async () => {
    vi.mocked(api.patch).mockResolvedValue({
      id: "1", email: "a@b.com", displayName: "New", avatarUrl: null,
      emailVerified: true, twoFactorEnabled: false,
    });
    const r = await AuthService.updateProfile({ displayName: "New", avatarUrl: null });
    expect(api.patch).toHaveBeenCalledWith("/auth/me", { displayName: "New", avatarUrl: null });
    expect(r.displayName).toBe("New");
    expect(r.emailVerified).toBe(true);
  });

  it("enroll2fa POSTs /auth/2fa/enroll and returns QR", async () => {
    vi.mocked(api.post).mockResolvedValue({ otpauthUrl: "otpauth://...", qrDataUri: "data:image/png;base64,x" });
    const r = await AuthService.enroll2fa();
    expect(api.post).toHaveBeenCalledWith("/auth/2fa/enroll", {});
    expect(r.qrDataUri).toBe("data:image/png;base64,x");
  });

  it("verify2faLogin POSTs mfaToken+code", async () => {
    vi.mocked(api.post).mockResolvedValue({ userId: "1", email: "a@b.com", displayName: "A", avatarUrl: null, expiresIn: 900 });
    await AuthService.verify2faLogin("mfatok", "123456");
    expect(api.post).toHaveBeenCalledWith("/auth/2fa/verify-login", { mfaToken: "mfatok", code: "123456" });
  });
});
```
> Nếu api-client hiện dùng response interceptor `unwrap` (return `res.data.data`) thì mock `unwrap: (e) => e.data` phản ánh đúng — kiểm tra file `frontend/lib/api-client.ts` hiện tại để đảm bảo không unwrap 2 lần.

- [ ] **Step 2: Run — verify fail (methods missing)**

Run: `cd frontend && npx vitest run src/services/api/__tests__/authService.test.ts`
Expected: FAIL (TS error / methods not defined)

- [ ] **Step 3: Modify `types/auth.ts`**

```ts
export interface AuthResponse {
  accessToken?: string;
  refreshToken?: string;
  userId: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  expiresIn: number;
  mfaRequired?: boolean;
  mfaToken?: string;
  twoFactorEnabled?: boolean;
  emailVerified?: boolean;
}

export interface UpdateProfileRequest {
  displayName?: string;
  avatarUrl?: string | null;
}

export interface Enroll2faResponse {
  otpauthUrl: string;
  qrDataUri: string;
}
```

- [ ] **Step 4: Modify `services/api/authService.ts`**

`ProfileResponse` (thay đổi trong file — thêm 2 field):
```ts
export interface ProfileResponse {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  emailVerified: boolean;
  twoFactorEnabled: boolean;
}
```
Thêm methods vào `AuthService` (cuối class trước closing brace):
```ts
  static async forgotPassword(email: string): Promise<void> {
    await api.post<{ message: string }>("/auth/forgot-password", { email });
  }

  static async resetPassword(token: string, newPassword: string): Promise<void> {
    await api.post<{ message: string }>("/auth/reset-password", { token, newPassword });
  }

  static async sendVerification(email: string): Promise<void> {
    await api.post<{ message: string }>("/auth/send-verification", { email });
  }

  static async verifyEmail(token: string): Promise<void> {
    await api.post<{ message: string }>("/auth/verify-email", { token });
  }

  static async updateProfile(body: UpdateProfileRequest): Promise<ProfileResponse> {
    return api.patch<ProfileResponse>("/auth/me", body);
  }

  static async enroll2fa(): Promise<Enroll2faResponse> {
    return api.post<Enroll2faResponse>("/auth/2fa/enroll", {});
  }

  static async verify2faSetup(code: string): Promise<void> {
    await api.post<{ message: string }>("/auth/2fa/verify", { code });
  }

  static async disable2fa(code: string): Promise<void> {
    await api.post<{ message: string }>("/auth/2fa/disable", { code });
  }

  static async verify2faLogin(mfaToken: string, code: string): Promise<AuthResponse> {
    return api.post<AuthResponse>("/auth/2fa/verify-login", { mfaToken, code });
  }
```
Import `UpdateProfileRequest, Enroll2faResponse` vào dòng import `@/types/auth` (thêm nếu chưa có — kiểm tra file hiện tại trước khi sửa).

- [ ] **Step 5: Run — verify pass**

Run: `cd frontend && npx vitest run src/services/api/__tests__/authService.test.ts`
Expected: PASS 6/6

- [ ] **Step 6: Commit**

```bash
git add frontend/types/auth.ts frontend/services/api/authService.ts frontend/services/api/__tests__/authService.test.ts
git commit -m "feat(fe): extend AuthService with 2FA/reset/profile methods"
```

---

### Task 11: FE validate helpers (pure, TDD)

**Files:**
- Create: `frontend/lib/validation/auth.ts`
- Create: `frontend/lib/validation/__tests__/auth.test.ts`

**Interfaces:**
- Produces: `validateEmail(email): string | null`, `validatePassword(pw): string | null`, `validateConfirmPassword(pw, confirm): string | null`, `validateDisplayName(name): string | null`, `validateTotpCode(code): string | null` — `null` = hợp lệ.

- [ ] **Step 1: Write failing test** `auth.test.ts`

```ts
import { describe, it, expect } from "vitest";
import {
  validateEmail,
  validatePassword,
  validateConfirmPassword,
  validateDisplayName,
  validateTotpCode,
} from "@/lib/validation/auth";

describe("validateEmail", () => {
  it("returns null for a valid email", () => {
    expect(validateEmail("user@example.com")).toBeNull();
  });
  it("returns a message for invalid email", () => {
    expect(validateEmail("not-an-email")).not.toBeNull();
    expect(validateEmail("")).not.toBeNull();
  });
});

describe("validatePassword", () => {
  it("accepts 8+ chars", () => {
    expect(validatePassword("password1")).toBeNull();
  });
  it("rejects short/blank", () => {
    expect(validatePassword("short")).not.toBeNull();
    expect(validatePassword("")).not.toBeNull();
  });
});

describe("validateConfirmPassword", () => {
  it("returns null when passwords match", () => {
    expect(validateConfirmPassword("password1", "password1")).toBeNull();
  });
  it("returns message when they differ", () => {
    expect(validateConfirmPassword("password1", "password2")).not.toBeNull();
  });
});

describe("validateDisplayName", () => {
  it("accepts non-blank", () => {
    expect(validateDisplayName("Quang")).toBeNull();
  });
  it("rejects blank", () => {
    expect(validateDisplayName("   ")).not.toBeNull();
  });
});

describe("validateTotpCode", () => {
  it("accepts 6 digits", () => {
    expect(validateTotpCode("123456")).toBeNull();
  });
  it("rejects non-6-digit", () => {
    expect(validateTotpCode("12345")).not.toBeNull();
    expect(validateTotpCode("abcdef")).not.toBeNull();
  });
});
```

- [ ] **Step 2: Run — verify fail (module missing)**

Run: `cd frontend && npx vitest run src/lib/validation/__tests__/auth.test.ts`
Expected: FAIL

- [ ] **Step 3: Write `lib/validation/auth.ts`**

```ts
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateEmail(email: string): string | null {
  if (!email.trim()) return "Email is required.";
  if (!EMAIL_RE.test(email.trim())) return "Enter a valid email address.";
  return null;
}

export function validatePassword(password: string): string | null {
  if (!password) return "Password is required.";
  if (password.length < 8) return "Password must be at least 8 characters.";
  return null;
}

export function validateConfirmPassword(password: string, confirm: string): string | null {
  if (password !== confirm) return "Passwords do not match.";
  return null;
}

export function validateDisplayName(name: string): string | null {
  if (!name.trim()) return "Display name is required.";
  return null;
}

export function validateTotpCode(code: string): string | null {
  if (!/^\d{6}$/.test(code.trim())) return "Enter the 6-digit code.";
  return null;
}
```

- [ ] **Step 4: Run — verify pass**

Run: `cd frontend && npx vitest run src/lib/validation/__tests__/auth.test.ts`
Expected: PASS 12/12

- [ ] **Step 5: Commit**

```bash
git add frontend/lib/validation/auth.ts frontend/lib/validation/__tests__/auth.test.ts
git commit -m "feat(fe): add pure auth validation helpers"
```

---

### Task 12: FE hooks mở rộng + store extension

**Files:**
- Modify: `frontend/hooks/useAuthStore.ts`
- Modify: `frontend/hooks/useAuth.ts`
- Create: `frontend/hooks/__tests__/useAuth.test.tsx`

**Interfaces:**
- Produces: hooks `useForgotPassword`, `useResetPassword`, `useVerifyEmail`, `useUpdateProfile`, `useEnroll2fa`, `useVerify2faSetup`, `useDisable2fa`, `useVerify2faLogin`, `useResendVerification`, `useBootstrapAuth`; store `User` + `emailVerified?`/`twoFactorEnabled?`.

- [ ] **Step 1: Modify `useAuthStore.ts`** — `User` + optional flags (để /account + BootstrapAuth đọc)

```ts
export interface User {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  emailVerified?: boolean;
  twoFactorEnabled?: boolean;
}
```
(`setAuth`/`clearAuth`/persist không đổi — `partialize` giữ nguyên, field mới optional nên localStorage cũ vẫn đọc.)

- [ ] **Step 2: Write failing test** `useAuth.test.tsx`

```tsx
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useAuthStore } from "@/hooks/useAuthStore";
import { useLogin, useUpdateProfile } from "@/hooks/useAuth";
import { AuthService } from "@/services/api/authService";

vi.mock("@/services/api/authService", () => ({
  AuthService: {
    login: vi.fn(),
    updateProfile: vi.fn(),
  },
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe("useLogin", () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({ user: null });
    vi.clearAllMocks();
  });

  it("sets auth when login succeeds (no 2FA)", async () => {
    vi.mocked(AuthService.login).mockResolvedValue({
      userId: "u1", email: "a@b.com", displayName: "A", avatarUrl: null, expiresIn: 900,
    });
    const { result } = renderHook(() => useLogin(), { wrapper });
    act(() => {
      result.current.mutate({ email: "a@b.com", password: "password1" });
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(useAuthStore.getState().user?.displayName).toBe("A");
  });

  it("does NOT set auth when login requires 2FA", async () => {
    vi.mocked(AuthService.login).mockResolvedValue({
      userId: "u1", email: "a@b.com", displayName: "A", avatarUrl: null,
      expiresIn: 0, mfaRequired: true, mfaToken: "mfatok", twoFactorEnabled: true,
    });
    const { result } = renderHook(() => useLogin(), { wrapper });
    act(() => {
      result.current.mutate({ email: "a@b.com", password: "password1" });
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(useAuthStore.getState().user).toBeNull();
    expect(result.current.data?.mfaRequired).toBe(true);
  });
});

describe("useUpdateProfile", () => {
  it("updates store user on success", async () => {
    useAuthStore.setState({ user: { id: "u1", email: "a@b.com", displayName: "Old", avatarUrl: null } });
    vi.mocked(AuthService.updateProfile).mockResolvedValue({
      id: "u1", email: "a@b.com", displayName: "New", avatarUrl: null,
      emailVerified: true, twoFactorEnabled: false,
    });
    const { result } = renderHook(() => useUpdateProfile(), { wrapper });
    act(() => {
      result.current.mutate({ displayName: "New", avatarUrl: null });
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(useAuthStore.getState().user?.displayName).toBe("New");
    expect(useAuthStore.getState().user?.twoFactorEnabled).toBe(false);
  });
});
```

- [ ] **Step 3: Run — fail (hooks/module missing)**

Run: `cd frontend && npx vitest run src/hooks/__tests__/useAuth.test.tsx`
Expected: FAIL

- [ ] **Step 4: Modify `useAuth.ts`**

Sửa `useLogin` onSuccess — early return khi mfaRequired (không setAuth/push):
```ts
    onSuccess: (data) => {
      if (data.mfaRequired) return; // bước 2 nhập TOTP code trong cùng trang (ADR D3)
      setAuth({
        id: data.userId,
        email: data.email,
        displayName: data.displayName,
        avatarUrl: data.avatarUrl,
      });
      router.push("/");
    },
```
Sửa `useRegister` — thêm toast nhắc verify email (D6):
```ts
  return useMutation({
    mutationFn: (data: RegisterRequest) => AuthService.register(data),
    onSuccess: (data) => {
      setAuth({
        id: data.userId,
        email: data.email,
        displayName: data.displayName,
        avatarUrl: data.avatarUrl,
      });
      toast.success("Account created", {
        description: `We sent a verification link to ${data.email}. Check your inbox.`,
      });
      router.push("/");
    },
  });
```
(import `toast` from "sonner" — nếu useAuth.ts chưa có.)

Thêm các hook mới (cuối file, trước closing brace):
```ts
export const useForgotPassword = () =>
  useMutation({
    mutationFn: (email: string) => AuthService.forgotPassword(email),
  });

export const useResetPassword = () =>
  useMutation({
    mutationFn: ({ token, newPassword }: { token: string; newPassword: string }) =>
      AuthService.resetPassword(token, newPassword),
  });

export const useVerifyEmail = () =>
  useMutation({
    mutationFn: (token: string) => AuthService.verifyEmail(token),
  });

export const useResendVerification = (email: string) =>
  useMutation({
    mutationFn: () => AuthService.sendVerification(email),
  });

export const useUpdateProfile = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  return useMutation({
    mutationFn: (body: UpdateProfileRequest) => AuthService.updateProfile(body),
    onSuccess: (data) => {
      setAuth({
        id: data.id,
        email: data.email,
        displayName: data.displayName,
        avatarUrl: data.avatarUrl,
        emailVerified: data.emailVerified,
        twoFactorEnabled: data.twoFactorEnabled,
      });
    },
  });
};

export const useEnroll2fa = () =>
  useMutation({ mutationFn: () => AuthService.enroll2fa() });

export const useVerify2faSetup = (onSuccess?: () => void) =>
  useMutation({
    mutationFn: (code: string) => AuthService.verify2faSetup(code),
    onSuccess,
  });

export const useDisable2fa = (onSuccess?: () => void) =>
  useMutation({
    mutationFn: (code: string) => AuthService.disable2fa(code),
    onSuccess,
  });

export const useVerify2faLogin = () => {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);
  return useMutation({
    mutationFn: ({ mfaToken, code }: { mfaToken: string; code: string }) =>
      AuthService.verify2faLogin(mfaToken, code),
    onSuccess: (data) => {
      setAuth({
        id: data.userId,
        email: data.email,
        displayName: data.displayName,
        avatarUrl: data.avatarUrl,
      });
      router.push("/");
    },
  });
};
```
Update imports cuối: thêm `UpdateProfileRequest` vào import `@/types/auth` (kiểm tra dòng sẵn có).

- [ ] **Step 5: `useBootstrapAuth`** (thêm cuối useAuth.ts)

```ts
export const useBootstrapAuth = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  return useCallback(async () => {
    try {
      const res = await AuthService.me();
      if (res.success && res.data) {
        setAuth({
          id: res.data.id,
          email: res.data.email,
          displayName: res.data.displayName,
          avatarUrl: res.data.avatarUrl,
          emailVerified: res.data.emailVerified,
          twoFactorEnabled: res.data.twoFactorEnabled,
        });
      } else {
        clearAuth();
      }
    } catch {
      // JWT hết hạn / chưa login — clear state cũ (fix localStorage stale, ADR D7)
      clearAuth();
    }
  }, [setAuth, clearAuth]);
};
```
(`useCallback` đã import ở đầu useAuth.ts — dùng chung.)

- [ ] **Step 6: Run — verify pass**

Run: `cd frontend && npx vitest run src/hooks/__tests__/useAuth.test.tsx`
Expected: PASS 3/3

- [ ] **Step 7: Commit**

```bash
git add frontend/hooks/useAuth.ts frontend/hooks/useAuthStore.ts frontend/hooks/__tests__/useAuth.test.tsx
git commit -m "feat(fe): add auth hooks for reset/verify/profile/2fa + bootstrap /me"
```

---

## Phase E — FE pages

### Task 13: Login 2FA step + forgot-password wired

**Files:**
- Modify: `frontend/app/(auth)/login/page.tsx`
- Modify: `frontend/app/(auth)/forgot-password/page.tsx`

**Interfaces:** `useLogin` (data.mfaRequired/mfaToken), `useVerify2faLogin`, `useForgotPassword`, `validateEmail`, `validateTotpCode`, `Input`/`Button`/`Label`/`Loader2` (đã có trong login page).

- [ ] **Step 1: Login 2FA step — modify `login/page.tsx`**

Thêm state + effect + nhánh render ngay đầu `return`. Trong component:
```tsx
  const [mfaToken, setMfaToken] = useState<string | null>(null);
  const [code, setCode] = useState("");
  const verify2faLogin = useVerify2faLogin();

  // Khi login trả mfaRequired → chuyển sang bước nhập 6 chữ số (giữ mfaToken trong memory — không persist)
  useEffect(() => {
    if (loginMutation.data?.mfaRequired && loginMutation.data.mfaToken) {
      setMfaToken(loginMutation.data.mfaToken);
    }
  }, [loginMutation.data]);

  const handleVerify2fa = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateTotpCode(code);
    if (err) return toast.error(err);
    if (!mfaToken) return toast.error("Session expired — log in again.");
    verify2faLogin.mutate(
      { mfaToken, code },
      {
        onError: (error) =>
          toast.error("Verification failed", { description: error.message || "Enter the 6-digit code from your app." }),
      }
    );
  };
```
Render — ở ĐẦU `return` của component, nhánh 2FA:
```tsx
  if (mfaToken) {
    return (
      <div className="flex flex-col items-center w-full max-w-[450px] mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-1000 transition-colors">
        <h1 className="text-4xl md:text-5xl font-bold text-center tracking-tighter text-foreground mb-2 leading-tight">
          Two-factor authentication
        </h1>
        <p className="text-muted-foreground text-sm text-center">
          Enter the 6-digit code from your authenticator app.
        </p>
        <form onSubmit={handleVerify2fa} className="w-full space-y-6">
          <Input
            id="2fa-code"
            type="text"
            inputMode="numeric"
            maxLength={6}
            placeholder="123456"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
            className="h-14 bg-background border-border text-foreground text-center text-2xl tracking-[0.5em] placeholder:text-muted-foreground rounded-[4px]"
          />
          <Button
            className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full transition-transform active:scale-[0.98] disabled:opacity-70"
            type="submit"
            disabled={verify2faLogin.isPending}
          >
            {verify2faLogin.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : "Verify"}
          </Button>
        </form>
      </div>
    );
  }
```
> import mới: `useEffect`, `validateTotpCode` từ `@/lib/validation/auth`. Giữ nguyên markup form login hiện có khi mfaToken null.

- [ ] **Step 2: Forgot-password wired — thay nội dung `forgot-password/page.tsx`**

```tsx
"use client";

import { useState } from "react";
import Link from "next/link";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { useForgotPassword } from "@/hooks/useAuth";
import { validateEmail } from "@/lib/validation/auth";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const forgotMutation = useForgotPassword();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateEmail(email);
    if (err) return toast.error(err);
    forgotMutation.mutate(email, {
      onError: (error) =>
        toast.error("Failed to send reset email", {
          description: error.message || "Please try again.",
        }),
      onSuccess: () => setSent(true),
    });
  };

  return (
    <div className="flex flex-col items-center w-full max-w-[450px] mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-1000 transition-colors">
      <h1 className="text-4xl md:text-5xl font-bold text-center tracking-tighter text-foreground mb-2 leading-tight">
        Reset your password
      </h1>
      {sent ? (
        <div className="w-full space-y-4 text-center">
          <p className="text-muted-foreground">
            If {email} is registered, we sent a reset link. Check your inbox and pick up where you left off.
          </p>
          <Button asChild className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full">
            <Link href="/login">Back to login</Link>
          </Button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="w-full space-y-6">
          <div className="space-y-2">
            <Label htmlFor="email" className="text-sm font-bold text-foreground">Email</Label>
            <Input
              id="email" type="email" placeholder="name@example.com" required
              value={email} onChange={(e) => setEmail(e.target.value)}
              className="h-12 bg-background border-border hover:border-foreground focus:border-foreground text-foreground placeholder:text-muted-foreground rounded-[4px]"
            />
          </div>
          <Button className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full" type="submit" disabled={forgotMutation.isPending}>
            {forgotMutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : "Send reset link"}
          </Button>
          <div className="text-center">
            <Link href="/login" className="text-sm text-foreground hover:text-spotify-green underline underline-offset-4 decoration-border">
              Back to login
            </Link>
          </div>
        </form>
      )}
    </div>
  );
}
```
> Trang này hiện là Server Component static (auth-split layout). Chuyển sang client — đúng vì cần state + mutation. Kiểm tra layout `(auth)` có import page này dạng static export nào không trước khi sửa.

- [ ] **Step 3: Verify frontend**

Run: `cd frontend && npx tsc --noEmit`
Expected: clean

- [ ] **Step 4: Commit**

```bash
git add "frontend/app/(auth)/login/page.tsx" "frontend/app/(auth)/forgot-password/page.tsx"
git commit -m "feat(fe): login 2FA step + wire forgot-password form"
```

---

### Task 14: /reset-password page + middleware whitelist

**Files:**
- Create: `frontend/app/(auth)/reset-password/page.tsx`
- Modify: `frontend/middleware.ts`

**Interfaces:** `useSearchParams` (token), `useResetPassword`, `validatePassword`, `validateConfirmPassword`.

- [ ] **Step 1: Write page** `reset-password/page.tsx`

```tsx
"use client";

import { useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useResetPassword } from "@/hooks/useAuth";
import { validatePassword, validateConfirmPassword } from "@/lib/validation/auth";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [done, setDone] = useState(false);
  const resetMutation = useResetPassword();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const pwErr = validatePassword(password);
    if (pwErr) return toast.error(pwErr);
    const cfErr = validateConfirmPassword(password, confirm);
    if (cfErr) return toast.error(cfErr);
    if (!token) return toast.error("This link is invalid or expired. Request a new one.");
    resetMutation.mutate(
      { token, newPassword: password },
      {
        onSuccess: () => setDone(true),
        onError: (error) =>
          toast.error("Reset failed", {
            description: error.message || "This link may have expired. Request a new one.",
          }),
      }
    );
  };

  if (done) {
    return (
      <div className="w-full text-center space-y-4">
        <h1 className="text-3xl font-bold tracking-tight text-foreground">Password updated</h1>
        <p className="text-muted-foreground">Your password has been reset. Log in with your new password.</p>
        <Button asChild className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full">
          <Link href="/login">Log in</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center w-full max-w-[450px] mx-auto space-y-8">
      <h1 className="text-4xl md:text-5xl font-bold text-center tracking-tighter text-foreground">
        Choose a new password
      </h1>
      <form onSubmit={handleSubmit} className="w-full space-y-6">
        <div className="space-y-2">
          <Label htmlFor="newPassword" className="text-sm font-bold text-foreground">New password</Label>
          <Input
            id="newPassword" type="password" placeholder="At least 8 characters" required
            value={password} onChange={(e) => setPassword(e.target.value)}
            className="h-12 bg-background border-border text-foreground placeholder:text-muted-foreground rounded-[4px]"
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="confirmPassword" className="text-sm font-bold text-foreground">Confirm password</Label>
          <Input
            id="confirmPassword" type="password" placeholder="Repeat password" required
            value={confirm} onChange={(e) => setConfirm(e.target.value)}
            className="h-12 bg-background border-border text-foreground placeholder:text-muted-foreground rounded-[4px]"
          />
        </div>
        <Button type="submit" disabled={resetMutation.isPending}
          className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full">
          {resetMutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : "Reset password"}
        </Button>
      </form>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="flex justify-center">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-spotify-green border-t-transparent" />
        </div>
      }
    >
      <ResetPasswordForm />
    </Suspense>
  );
}
```
> `Suspense` bọc `useSearchParams` — yêu cầu Next.js App Router (pre-render).

- [ ] **Step 2: Modify `middleware.ts`** — whitelist 3 page công khai

Thay block `isAuthPage`:
```ts
  const isAuthPage =
    request.nextUrl.pathname.startsWith('/login') ||
    request.nextUrl.pathname.startsWith('/register') ||
    request.nextUrl.pathname.startsWith('/forgot-password') ||
    request.nextUrl.pathname.startsWith('/reset-password') ||
    request.nextUrl.pathname.startsWith('/verify-email');
```
> KHÔNG thêm `/oauth2/callback` — giữ nguyên hành vi hiện tại (middleware cho callback qua; sau OAuth2 redirect có cookie, callback tự `me()` bootstrap).

- [ ] **Step 3: Verify**

Run: `cd frontend && npx tsc --noEmit`
Expected: clean

- [ ] **Step 4: Commit**

```bash
git add "frontend/app/(auth)/reset-password/page.tsx" frontend/middleware.ts
git commit -m "feat(fe): add reset-password page + whitelist public routes"
```

---

### Task 15: /verify-email page

**Files:**
- Create: `frontend/app/(auth)/verify-email/page.tsx`

**Interfaces:** `AuthService.verifyEmail`, `useAuthStore`, `useSearchParams`.

- [ ] **Step 1: Write page** `verify-email/page.tsx`

```tsx
"use client";

import { useEffect, useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { AuthService } from "@/services/api/authService";
import { useAuthStore } from "@/hooks/useAuthStore";

type Status = "verifying" | "success" | "error";

function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const [status, setStatus] = useState<Status>("verifying");
  const user = useAuthStore((s) => s.user);

  useEffect(() => {
    let cancelled = false;
    if (!token) {
      setStatus("error");
      return;
    }
    AuthService.verifyEmail(token)
      .then(() => { if (!cancelled) setStatus("success"); })
      .catch(() => { if (!cancelled) setStatus("error"); });
    return () => { cancelled = true; };
  }, [token]);

  if (status === "verifying") {
    return (
      <div className="flex flex-col items-center gap-4">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-spotify-green border-t-transparent" />
        <p className="text-foreground">Verifying your email…</p>
      </div>
    );
  }
  if (status === "success") {
    return (
      <div className="space-y-4 text-center">
        <h1 className="text-3xl font-bold text-foreground">Email verified</h1>
        <p className="text-muted-foreground">Your account email has been confirmed.</p>
        <Button asChild className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full">
          <Link href={user ? "/" : "/login"}>{user ? "Back to home" : "Log in"}</Link>
        </Button>
      </div>
    );
  }
  return (
    <div className="space-y-4 text-center">
      <h1 className="text-3xl font-bold text-foreground">Verification link invalid</h1>
      <p className="text-muted-foreground">This link may have expired. Request a new verification email.</p>
      <Button asChild className="w-full bg-spotify-green hover:opacity-90 text-black font-bold h-12 rounded-full">
        <Link href="/account">Go to account</Link>
      </Button>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense
      fallback={
        <div className="flex justify-center">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-spotify-green border-t-transparent" />
        </div>
      }
    >
      <VerifyEmailContent />
    </Suspense>
  );
}
```
> Sau verify thành công, `user.emailVerified` trong store chưa refresh ngay — banner /account cập nhật ở lần `/me` kế (ADR D7 revalidate khi mount). Chấp nhận cho đợt này.

- [ ] **Step 2: Verify**

Run: `cd frontend && npx tsc --noEmit`
Expected: clean

- [ ] **Step 3: Commit**

```bash
git add "frontend/app/(auth)/verify-email/page.tsx"
git commit -m "feat(fe): add verify-email page"
```

---

### Task 16: /account page + BootstrapAuth + TopNav wiring

**Files:**
- Create: `frontend/components/providers/BootstrapAuth.tsx`
- Modify: `frontend/app/layout.tsx`
- Create: `frontend/app/(main)/account/page.tsx`
- Modify: `frontend/components/TopNav.tsx`

**Interfaces:** `useBootstrapAuth`, `useCurrentUser`, `useUpdateProfile`, `useEnroll2fa`, `useVerify2faSetup`, `useDisable2fa`, `useResendVerification`, `validateDisplayName`, `validateTotpCode`.

- [ ] **Step 1: BootstrapAuth** (client wrapper mount `/me` — ADR D7)

Create `frontend/components/providers/BootstrapAuth.tsx`:
```tsx
"use client";

import { useEffect } from "react";
import { useBootstrapAuth } from "@/hooks/useAuth";

export function BootstrapAuth({ children }: { children: React.ReactNode }) {
  const bootstrap = useBootstrapAuth();

  // Revalidate auth state sau hydrate để fix localStorage stale (JWT chết nhưng UI còn user)
  useEffect(() => {
    bootstrap();
  }, [bootstrap]);

  return <>{children}</>;
}
```

- [ ] **Step 2: Modify `app/layout.tsx`** — bọc children

Import `BootstrapAuth`; trong JSX (trong `QueryProvider`), bọc children:
```tsx
          <QueryProvider>
            <BootstrapAuth>{children}</BootstrapAuth>
            <Toaster position="top-center" richColors />
          </QueryProvider>
```

- [ ] **Step 3: `/account` page** — tạo `app/(main)/account/page.tsx`

```tsx
"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  useCurrentUser,
  useUpdateProfile,
  useEnroll2fa,
  useVerify2faSetup,
  useDisable2fa,
  useResendVerification,
} from "@/hooks/useAuth";
import { validateDisplayName, validateTotpCode } from "@/lib/validation/auth";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

export default function AccountPage() {
  const user = useCurrentUser();
  // BootstrapAuth revalidate /me khi mount — emailVerified/twoFactorEnabled lấy từ store
  const emailVerified = user?.emailVerified ?? true;
  const twoFactorEnabled = user?.twoFactorEnabled ?? false;

  const [displayName, setDisplayName] = useState(user?.displayName ?? "");
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl ?? "");
  const [qr, setQr] = useState<{ otpauthUrl: string; qrDataUri: string } | null>(null);
  const [code, setCode] = useState("");
  const [disableCode, setDisableCode] = useState("");
  const [resendSent, setResendSent] = useState(false);

  const profileMutation = useUpdateProfile();
  const enrollMutation = useEnroll2fa();
  const verifySetupMutation = useVerify2faSetup(() => {
    setQr(null);
    setCode("");
    toast.success("Two-factor authentication enabled");
  });
  const disableMutation = useDisable2fa(() => {
    setDisableCode("");
    toast.success("Two-factor authentication disabled");
  });
  const resendMutation = useResendVerification(user?.email ?? "");

  const handleProfile = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateDisplayName(displayName);
    if (err) return toast.error(err);
    profileMutation.mutate(
      { displayName: displayName.trim(), avatarUrl: avatarUrl.trim() || null },
      {
        onSuccess: () => toast.success("Profile updated"),
        onError: (error) => toast.error("Update failed", { description: error.message || "Please try again." }),
      }
    );
  };

  const handleEnroll = () => {
    enrollMutation.mutate(undefined, {
      onSuccess: (data) => {
        setQr(data);
        toast.info("Scan the QR code with your authenticator app");
      },
      onError: (error) => toast.error("Could not start 2FA setup", { description: error.message }),
    });
  };

  const handleVerifySetup = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateTotpCode(code);
    if (err) return toast.error(err);
    verifySetupMutation.mutate(code);
  };

  const handleDisable = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateTotpCode(disableCode);
    if (err) return toast.error(err);
    disableMutation.mutate(disableCode, {
      onError: (error) => toast.error("Could not disable 2FA", { description: error.message }),
    });
  };

  return (
    <div className="mx-auto w-full max-w-2xl space-y-8 px-4 py-8">
      <h1 className="text-3xl font-bold tracking-tight text-foreground">Account</h1>

      {/* Email verification banner (D6) */}
      {!emailVerified && (
        <div className="flex items-center justify-between gap-4 rounded-lg border border-border bg-background px-4 py-3">
          <div>
            <p className="text-sm font-bold text-foreground">Email not verified</p>
            <p className="text-xs text-muted-foreground">Verify {user?.email} to secure your account.</p>
          </div>
          <Button
            variant="outline" size="sm"
            onClick={() => {
              resendMutation.mutate(undefined, {
                onSuccess: () => { setResendSent(true); toast.success("Verification email sent"); },
              });
            }}
          >
            {resendSent ? "Resent" : "Resend email"}
          </Button>
        </div>
      )}

      {/* Profile form */}
      <section className="space-y-4 rounded-lg border border-border bg-background p-6">
        <h2 className="text-lg font-bold text-foreground">Profile</h2>
        <form onSubmit={handleProfile} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="displayName" className="text-sm font-bold text-foreground">Display name</Label>
            <Input id="displayName" value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="h-11 bg-background border-border text-foreground rounded-[4px]" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="avatarUrl" className="text-sm font-bold text-foreground">Avatar URL</Label>
            <Input id="avatarUrl" type="url" placeholder="https://example.com/avatar.png" value={avatarUrl}
              onChange={(e) => setAvatarUrl(e.target.value)}
              className="h-11 bg-background border-border text-foreground rounded-[4px]" />
            <p className="text-xs text-muted-foreground">Image URL text — no file upload (đã chốt scope).</p>
          </div>
          <Button type="submit" disabled={profileMutation.isPending}
            className="bg-spotify-green hover:opacity-90 text-black font-bold h-10 rounded-full px-6">
            {profileMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : "Save changes"}
          </Button>
        </form>
      </section>

      {/* 2FA section */}
      <section className="space-y-4 rounded-lg border border-border bg-background p-6">
        <h2 className="text-lg font-bold text-foreground">Two-factor authentication</h2>

        {!twoFactorEnabled ? (
          !qr ? (
            <div className="flex items-center justify-between gap-4">
              <p className="text-sm text-muted-foreground">
                2FA is disabled. Enable it to add an extra layer of security with an authenticator app.
              </p>
              <Button onClick={handleEnroll} disabled={enrollMutation.isPending}
                className="bg-spotify-green hover:opacity-90 text-black font-bold h-10 rounded-full px-6">
                Set up 2FA
              </Button>
            </div>
          ) : (
            <div className="space-y-4">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={qr.qrDataUri} alt="TOTP QR code" className="mx-auto h-48 w-48" />
              <p className="text-xs text-muted-foreground text-center">
                Scan with Google Authenticator, then enter the 6-digit code.
              </p>
              <form onSubmit={handleVerifySetup} className="mx-auto flex max-w-xs gap-2">
                <Input inputMode="numeric" maxLength={6} placeholder="123456" value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
                  className="h-11 bg-background border-border text-foreground text-center tracking-[0.3em] rounded-[4px]" />
                <Button type="submit" disabled={verifySetupMutation.isPending}
                  className="bg-spotify-green hover:opacity-90 text-black font-bold h-11 rounded-full px-5">
                  Verify
                </Button>
              </form>
            </div>
          )
        ) : (
          <form onSubmit={handleDisable} className="space-y-3">
            <p className="text-sm text-muted-foreground">2FA is enabled. Enter your current code to turn it off.</p>
            <div className="flex max-w-xs gap-2">
              <Input inputMode="numeric" maxLength={6} placeholder="123456" value={disableCode}
                onChange={(e) => setDisableCode(e.target.value.replace(/\D/g, ""))}
                className="h-11 bg-background border-border text-foreground text-center tracking-[0.3em] rounded-[4px]" />
              <Button type="submit" variant="outline" disabled={disableMutation.isPending}>
                Disable
              </Button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}
```
> `enrollMutation.mutate(undefined, {...})` — mutationFn `() => AuthService.enroll2fa()` chấp nhận undefined argument. `user?.emailVerified ?? true` để tránh banner flash với user OAuth2 trước khi bootstrap chạy.
> `useCurrentUser` — kiểm tra file `useAuth.ts` có export sẵn chưa; nếu chưa, export `export const useCurrentUser = () => useAuthStore((s) => s.user);` (granular selector).

- [ ] **Step 4: TopNav wire dropdown** — Account/Profile/Settings → `/account`

Modify `frontend/components/TopNav.tsx` avatar dropdown: các `DropdownMenuItem` Account/Profile/Settings thay bằng (đúng pattern `asChild` + `<Link>`):
```tsx
        <DropdownMenuItem asChild>
          <Link href="/account">Account</Link>
        </DropdownMenuItem>
```
(Profile/Settings cũng trỏ `/account`). Item "Private session" giữ nguyên trang trí (visual-only, ngoài scope). Import `Link` từ `next/link` nếu chưa có — kiểm tra file hiện tại.

- [ ] **Step 5: Verify**

Run: `cd frontend && npx tsc --noEmit`
Run: `cd frontend && npm run lint`
Run: `cd frontend && npx vitest run src/hooks src/services/api src/lib/validation`
Run: `cd frontend && npm run build` (structural: route mới + layout thay đổi)
Expected: tsc/lint/vitest/build đều xanh

- [ ] **Step 6: Commit**

```bash
git add frontend/components/providers/BootstrapAuth.tsx frontend/app/layout.tsx "frontend/app/(main)/account/page.tsx" frontend/components/TopNav.tsx
git commit -m "feat(fe): add account settings page (profile + 2FA + email banner)"
```

---

### Task 17: Register Google onClick + docs update + final gate

**Files:**
- Modify: `frontend/app/(auth)/register/page.tsx`
- Modify: `PROJECT_STATUS.md`
- Modify: `.claude/rules/context.md`

- [ ] **Step 1: Register Google onClick**

Thêm constant + handler (giống login page):
```tsx
const GATEWAY_URL = process.env.NEXT_PUBLIC_GATEWAY_URL || "http://localhost:9000";

const handleGoogleLogin = () => {
  // Redirect to Gateway OAuth2 (Google) — toàn trang để nhận HttpOnly cookies.
  window.location.href = `${GATEWAY_URL}/oauth2/authorization/google`;
};
```
Gắn vào `<SocialButton onClick={handleGoogleLogin} ...>` (import SocialButton nếu chưa có — dùng đúng component đã render trong register page).

- [ ] **Step 2: Update docs**

`PROJECT_STATUS.md` — ghi phase mới (auth completion), inventory: auth-service bullet cập nhật: "TOTP 2FA (local), PATCH /me profile, email verification auto-send", test counts sau gate. `.claude/rules/context.md` — auth-service row mô tả thêm "TOTP 2FA local, profile PATCH /me, email verification auto-send".

- [ ] **Step 3: Final verify toàn bộ**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn test -pl auth-service -am`
Run: `cd frontend && npx tsc --noEmit && npm run lint`
Run: `cd frontend && npx vitest run src`
Expected: backend auth test xanh (đếm và ghi lại — dự kiến 13 cũ + ~20 mới), FE tsc/lint/vitest xanh (ghi số test)

- [ ] **Step 4: Commit**

```bash
git add "frontend/app/(auth)/register/page.tsx" PROJECT_STATUS.md .claude/rules/context.md
git commit -m "feat(fe): wire google login on register + update project status"
```

---

## Self-Review kết quả (chạy sau Task 17 — trước khi merge)

- **Backend:** auth-service test toàn bộ xanh; Clean Arch self-check (domain/ không import Spring — kiểm tra `User.java` chỉ imports java.*; application/ không import Spring ngoài annotation `@Service`/`@Transactional`); gateway compile OK.
- **Frontend:** `tsc --noEmit` clean, lint clean, vitest xanh, `npm run build` OK.
- **Smoke thủ công (spec §8 E2E — không tự động, cần docker + SMTP Gmail thật):** register → nhận mail verify → click link → banner mất; forgot → mail reset → reset password → login; bật 2FA trên /account (scan QR, nhập code) → logout → login → nhập code.
- **Docs:** `PROJECT_STATUS.md` cập nhật phase + test count + next actions.

## Merge handoff
- Sau khi gate xanh: `finishing-a-development-branch` skill → merge `feature/auth-completion` vào `main` (chờ user chọn option).