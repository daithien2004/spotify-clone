package com.spotify.auth.infrastructure.security.oauth2;

import java.util.Map;

/**
 * Abstraction over different OAuth2 provider's user info responses.
 */
public interface OAuth2UserInfo {
    String getProvider();
    String getProviderId();
    String getEmail();
    String getDisplayName();
    String getAvatarUrl();
    Map<String, Object> getAttributes();
}
