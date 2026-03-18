package com.spotify.auth.application.port.out;

import com.spotify.auth.domain.entity.User;

public interface TokenPort {
    String generateToken(User user);
    String getEmailFromToken(String token);
    boolean validateToken(String token);
}
