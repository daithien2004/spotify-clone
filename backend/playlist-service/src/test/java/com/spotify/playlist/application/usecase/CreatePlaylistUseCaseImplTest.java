package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistResponse;
import com.spotify.playlist.application.port.UserProfileClient;
import com.spotify.playlist.domain.repository.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePlaylistUseCaseImplTest {

    @Mock private PlaylistRepository playlistRepository;
    @Mock private UserProfileClient userProfileClient;
    @InjectMocks private CreatePlaylistUseCaseImpl useCase;

    @Test
    void should_SavePlaylistWithResolvedOwner_when_UserProfileKnown() {
        UUID ownerUserId = UUID.randomUUID();
        when(userProfileClient.findDisplayName(ownerUserId)).thenReturn(Optional.of("Alice"));
        var saved = com.spotify.playlist.domain.entity.Playlist.builder()
                .id(UUID.randomUUID()).title("Chill Hits").description("relax")
                .ownerName("Alice").createdAt(java.time.OffsetDateTime.now())
                .updatedAt(java.time.OffsetDateTime.now()).build();
        when(playlistRepository.save(any())).thenReturn(saved);

        PlaylistResponse result = useCase.execute(
                new CreatePlaylistUseCase.CreatePlaylistCommand(ownerUserId, "Chill Hits", "relax"));

        assertThat(result.ownerName()).isEqualTo("Alice");
        verify(playlistRepository).save(any());
    }

    @Test
    void should_FallBackToYou_when_UserNotYetSynced() {
        UUID ownerUserId = UUID.randomUUID();
        when(userProfileClient.findDisplayName(ownerUserId)).thenReturn(Optional.empty());
        com.spotify.playlist.domain.entity.Playlist saved = com.spotify.playlist.domain.entity.Playlist.builder()
                .id(UUID.randomUUID()).title("My List").ownerName("You")
                .createdAt(java.time.OffsetDateTime.now())
                .updatedAt(java.time.OffsetDateTime.now()).build();
        when(playlistRepository.save(any())).thenReturn(saved);

        PlaylistResponse result = useCase.execute(
                new CreatePlaylistUseCase.CreatePlaylistCommand(ownerUserId, "My List", null));

        assertThat(result.ownerName()).isEqualTo("You");
        verify(playlistRepository).save(any());
    }
}
