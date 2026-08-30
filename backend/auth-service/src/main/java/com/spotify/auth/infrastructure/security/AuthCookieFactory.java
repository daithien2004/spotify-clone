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
