package com.spotify.auth.infrastructure.security.oauth2;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Loads and processes the authenticated OAuth2 user.
 * Creates a new user or links to existing account based on email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"
        OAuth2UserInfo userInfo = extractUserInfo(registrationId, oAuth2User);

        User user = findOrCreateUser(userInfo);

        return new CustomOAuth2Principal(user, oAuth2User.getAttributes());
    }

    private OAuth2UserInfo extractUserInfo(String registrationId, OAuth2User oAuth2User) {
        if ("google".equals(registrationId)) {
            return new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
        }
        throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
    }

    private User findOrCreateUser(OAuth2UserInfo userInfo) {
        Email email = new Email(userInfo.getEmail());

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Account linking: if email exists with different provider, allow login but don't override
            log.info("OAuth2 login for existing user: {}", email.value());
            return user;
        }

        // Create new user from OAuth2 info
        User newUser = User.fromOAuth2(
                userInfo.getEmail(),
                userInfo.getDisplayName(),
                userInfo.getAvatarUrl(),
                userInfo.getProvider(),
                userInfo.getProviderId()
        );
        log.info("Creating new OAuth2 user: {}", newUser.getEmail().value());
        return userRepository.save(newUser);
    }
}
