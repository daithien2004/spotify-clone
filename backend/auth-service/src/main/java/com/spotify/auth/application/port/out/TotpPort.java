package com.spotify.auth.application.port.out;

/** Port cho TOTP 2FA — infrastructure adapter dùng dev.samstevens.totp. */
public interface TotpPort {

  String generateSecret();

  String buildOtpAuthUri(String account, String issuer, String secret);

  /** Base64 PNG data URI của QR code — FE <img src> render trực tiếp. */
  String generateQrDataUri(String otpauthUri);

  boolean isValid(String code, String secret);
}