package com.spotify.auth.application.port.out;

import com.spotify.auth.domain.entity.User;

import java.util.Date;

public interface TokenPort {
    String generateToken(User user);
    String generateRefreshToken();
    long getRefreshTokenExpirationMillis();
    String getEmailFromToken(String token);
    Date getExpirationFromToken(String token);
    boolean validateToken(String token);
}
