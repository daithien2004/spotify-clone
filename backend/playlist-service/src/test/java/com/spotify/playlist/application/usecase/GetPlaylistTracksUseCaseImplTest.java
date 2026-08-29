package com.spotify.playlist.application.usecase;

import com.spotify.playlist.application.dto.PlaylistTrackResponse;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPlaylistTracksUseCaseImplTest {

    @Mock
    private PlaylistTrackRepository playlistTrackRepository;

    @InjectMocks
    private GetPlaylistTracksUseCaseImpl useCase;

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
    void should_ReturnEmptyList_when_PlaylistHasNoTracks() {
        UUID playlistId = UUID.randomUUID();
        when(playlistTrackRepository.findAllByPlaylistId(playlistId)).thenReturn(List.of());

        List<PlaylistTrackResponse> result = useCase.execute(playlistId);

        assertTrue(result.isEmpty());
    }

    @Test
    void should_ReturnAllTracksInRankOrder_when_PlaylistHasTracks() {
        UUID playlistId = UUID.randomUUID();
        PlaylistTrack first = trackOf(playlistId, "a");
        PlaylistTrack second = trackOf(playlistId, "m");
        when(playlistTrackRepository.findAllByPlaylistId(playlistId)).thenReturn(List.of(first, second));

        List<PlaylistTrackResponse> result = useCase.execute(playlistId);

        assertEquals(2, result.size());
        assertEquals(second.getId(), result.get(1).id()); // repo already orders by rank
        assertEquals(first.getTrackId(), result.get(0).trackId());
    }
}