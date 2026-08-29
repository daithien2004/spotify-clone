package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistResponse;
import com.spotify.playlist.domain.entity.Playlist;
import com.spotify.playlist.domain.exception.PlaylistNotFoundException;
import com.spotify.playlist.domain.repository.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPlaylistByIdUseCaseImplTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @InjectMocks
    private GetPlaylistByIdUseCaseImpl useCase;

    @Test
    void should_ReturnPlaylist_when_PlaylistExists() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = playlistOf(playlistId, "Chill Mix");
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        PlaylistResponse result = useCase.execute(playlistId);

        assertEquals(playlistId, result.id());
        assertEquals("Chill Mix", result.title());
    }

    @Test
    void should_ThrowPlaylistNotFound_when_PlaylistMissing() {
        UUID playlistId = UUID.randomUUID();
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThrows(PlaylistNotFoundException.class, () -> useCase.execute(playlistId));
    }

    private Playlist playlistOf(UUID id, String title) {
        return Playlist.builder()
                .id(id)
                .title(title)
                .ownerName("Spotify")
                .build();
    }
}