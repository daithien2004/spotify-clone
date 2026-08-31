package com.spotify.auth.infrastructure.security;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.spotify.auth.application.port.out.TotpPort;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Adapter TOTP — wrap SecretGenerator/CodeVerifier/QrGenerator (beans TotpConfig). */
@Component
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
    // totp 1.7.1: QrData lacks QrData.fromUri(String) — rebuild QrData by parsing the otpauth URI.
    try {
      QrData qrData = buildQrDataFromUri(otpauthUri);
      byte[] bytes = qrGenerator.generate(qrData);
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    } catch (Exception e) {
      throw new IllegalStateException("Could not generate QR code", e);
    }
  }

  private QrData buildQrDataFromUri(String otpauthUri) throws URISyntaxException {
    URI uri = new URI(otpauthUri);
    Map<String, String> params = splitQuery(uri.getRawQuery());
    String label = uri.getPath().startsWith("/") ? uri.getPath().substring(1) : uri.getPath();
    return new QrData.Builder()
        .issuer(params.getOrDefault("issuer", ""))
        .label(label)
        .secret(params.get("secret"))
        .digits(Integer.parseInt(params.getOrDefault("digits", "6")))
        .period(Integer.parseInt(params.getOrDefault("period", "30")))
        .build();
  }

  private Map<String, String> splitQuery(String rawQuery) {
    Map<String, String> params = new HashMap<>();
    if (rawQuery == null) {
      return params;
    }
    for (String pair : rawQuery.split("&")) {
      int idx = pair.indexOf('=');
      if (idx < 0) {
        continue;
      }
      params.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
          URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
    }
    return params;
  }

  @Override
  public boolean isValid(String code, String secret) {
    return codeVerifier.isValidCode(secret, code);
  }
}