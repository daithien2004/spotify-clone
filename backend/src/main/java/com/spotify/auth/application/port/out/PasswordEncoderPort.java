package com.spotify.auth.application.port.out;

import java.util.UUID;

public interface PasswordEncoderPort {
    String encode(String password);
    boolean matches(String rawPassword, String encodedPassword);
}
