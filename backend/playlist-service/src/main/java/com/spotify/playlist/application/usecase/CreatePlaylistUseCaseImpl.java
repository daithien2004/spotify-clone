package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistResponse;
import com.spotify.playlist.application.port.UserProfileClient;
import com.spotify.playlist.domain.entity.Playlist;
import com.spotify.playlist.domain.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePlaylistUseCaseImpl implements CreatePlaylistUseCase {

    /** Fallback owner label for users not yet synced into user-service (Kafka lag). */
    private static final String FALLBACK_OWNER = "You";

    private final PlaylistRepository playlistRepository;
    private final UserProfileClient userProfileClient;

    @Override
    @Transactional
    public PlaylistResponse execute(CreatePlaylistCommand command) {
        // Resolve display name from user-service; fall back to "You" if the user
        // hasn't been projected yet (idempotent consumer may lag the registration event).
        String ownerName = userProfileClient.findDisplayName(command.ownerUserId())
                .orElse(FALLBACK_OWNER);

        Playlist playlist = Playlist.builder()
                .id(UUID.randomUUID())
                .title(command.title())
                .description(command.description())
                .ownerName(ownerName)
                .coverUrl(null)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return PlaylistResponse.from(playlistRepository.save(playlist));
    }
}
