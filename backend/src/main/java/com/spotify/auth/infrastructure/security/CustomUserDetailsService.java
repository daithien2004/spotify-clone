package com.spotify.auth.infrastructure.security;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Cacheable(value = "userDetails", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(new Email(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        // In the future, if your User entity has a Roles collection, you would map them here.
        // For now, we grant a default ROLE_USER to every valid, authenticated user.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail().value(),
                user.getPassword().value(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
