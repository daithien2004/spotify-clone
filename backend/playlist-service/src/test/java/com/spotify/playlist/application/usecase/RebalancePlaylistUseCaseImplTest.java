package com.spotify.playlist.application.usecase;

import com.spotify.playlist.domain.entity.PlaylistTrack;
import com.spotify.playlist.domain.repository.PlaylistTrackRepository;
import com.spotify.playlist.domain.valueobject.LexoRank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RebalancePlaylistUseCaseImplTest {

    @Mock
    private PlaylistTrackRepository playlistTrackRepository;

    @InjectMocks
    private RebalancePlaylistUseCaseImpl useCase;

    private PlaylistTrack trackOf(UUID playlistId, String rank) {
        return PlaylistTrack.builder()
                .id(UUID.randomUUID())
                .playlistId(playlistId)
                .trackId(UUID.randomUUID())
                .lexoRank(new LexoRank(rank))
                .addedAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void should_RebalanceOnlyTracksOfGivenPlaylist() {
        UUID playlistId = UUID.randomUUID();
        List<PlaylistTrack> tracks = List.of(
                trackOf(playlistId, "m"),
                trackOf(playlistId, "z"));
        when(playlistTrackRepository.findAllByPlaylistId(playlistId)).thenReturn(tracks);

        useCase.execute(playlistId);

        // Only exactly the playlist tracks are touched, never the whole table.
        verify(playlistTrackRepository).findAllByPlaylistId(playlistId);
        verify(playlistTrackRepository).saveAll(tracks);
        assertEquals(new LexoRank("00001000"), tracks.get(0).getLexoRank());
        assertEquals(new LexoRank("00002000"), tracks.get(1).getLexoRank());
    }
}