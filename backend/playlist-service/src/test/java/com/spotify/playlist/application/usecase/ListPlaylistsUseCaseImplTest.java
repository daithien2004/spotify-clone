package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistSummaryResponse;
import com.spotify.playlist.domain.entity.Playlist;
import com.spotify.playlist.domain.repository.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPlaylistsUseCaseImplTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @InjectMocks
    private ListPlaylistsUseCaseImpl useCase;

    @Test
    void should_ReturnEmptyList_when_NoPlaylists() {
        when(playlistRepository.findAll()).thenReturn(List.of());

        List<PlaylistSummaryResponse> result = useCase.execute();

        assertEquals(0, result.size());
    }

    @Test
    void should_ReturnAllPlaylists() {
        Playlist chill = playlistOf(UUID.randomUUID(), "Chill Mix");
        Playlist pop = playlistOf(UUID.randomUUID(), "Pop Mix");
        when(playlistRepository.findAll()).thenReturn(List.of(chill, pop));

        List<PlaylistSummaryResponse> result = useCase.execute();

        assertEquals(2, result.size());
        assertEquals("Chill Mix", result.get(0).title());
        assertEquals("Pop Mix", result.get(1).title());
    }

    private Playlist playlistOf(UUID id, String title) {
        return Playlist.builder()
                .id(id)
                .title(title)
                .ownerName("Spotify")
                .build();
    }
}