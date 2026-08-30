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