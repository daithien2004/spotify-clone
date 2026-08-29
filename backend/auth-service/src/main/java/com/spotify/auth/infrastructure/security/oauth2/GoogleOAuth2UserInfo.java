package com.spotify.auth.infrastructure.security.oauth2;

import java.util.Map;

/**
 * Google-specific OAuth2 user info extracted from the Google token.
 */
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub"); // Google's unique user ID
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getDisplayName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getAvatarUrl() {
        return (String) attributes.get("picture");
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
