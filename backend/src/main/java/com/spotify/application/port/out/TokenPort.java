package com.spotify.application.port.out;

import com.spotify.domain.entity.User;

public interface TokenPort {
    String generateToken(User user);
}
