package com.spotify.user.presentation.controller;

import com.spotify.user.application.dto.FollowSummaryResponse;
import com.spotify.user.application.usecase.FollowUserUseCase;
import com.spotify.user.application.usecase.UnfollowUserUseCase;
import com.spotify.user.application.usecase.ViewFollowersUseCase;
import com.spotify.user.application.usecase.ViewFollowingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{id}")
@RequiredArgsConstructor
public class FollowController {

    private final FollowUserUseCase followUserUseCase;
    private final UnfollowUserUseCase unfollowUserUseCase;
    private final ViewFollowersUseCase viewFollowersUseCase;
    private final ViewFollowingUseCase viewFollowingUseCase;

    @GetMapping("/followers")
    public ResponseEntity<List<FollowSummaryResponse>> followers(@PathVariable UUID id) {
        return ResponseEntity.ok(viewFollowersUseCase.execute(id));
    }

    @GetMapping("/following")
    public ResponseEntity<List<FollowSummaryResponse>> following(@PathVariable UUID id) {
        return ResponseEntity.ok(viewFollowingUseCase.execute(id));
    }

    @PostMapping("/follow")
    public ResponseEntity<Void> follow(@PathVariable UUID id, Authentication authentication) {
        UUID followerId = UUID.fromString(authentication.getName());
        followUserUseCase.execute(followerId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/follow")
    public ResponseEntity<Void> unfollow(@PathVariable UUID id, Authentication authentication) {
        UUID followerId = UUID.fromString(authentication.getName());
        unfollowUserUseCase.execute(followerId, id);
        return ResponseEntity.ok().build();
    }
}
