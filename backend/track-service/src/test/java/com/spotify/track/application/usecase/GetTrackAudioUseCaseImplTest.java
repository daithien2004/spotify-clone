package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.GetTrackAudioCommand;
import com.spotify.track.domain.entity.TrackAudioRange;
import com.spotify.track.domain.repository.TrackAudioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTrackAudioUseCaseImplTest {

    @Mock
    private TrackAudioRepository trackAudioRepository;

    @InjectMocks
    private GetTrackAudioUseCaseImpl useCase;

    @Test
    void should_ReturnRequestedRange_when_RepositoryHasAudio() {
        UUID trackId = UUID.randomUUID();
        TrackAudioRange range = new TrackAudioRange(
                new ByteArrayInputStream(new byte[1024]), 0, 1024, 4096, "audio/mpeg");
        when(trackAudioRepository.getAudio(trackId, 0, 1024)).thenReturn(range);

        TrackAudioRange result = useCase.execute(new GetTrackAudioCommand(trackId, 0, 1024));

        assertEquals(1024, result.length());
        assertEquals(4096, result.totalSize());
    }

    @Test
    void should_PassThroughPartialOffset() {
        UUID trackId = UUID.randomUUID();
        TrackAudioRange range = new TrackAudioRange(
                new ByteArrayInputStream(new byte[512]), 1024, 512, 4096, "audio/mpeg");
        when(trackAudioRepository.getAudio(trackId, 1024, 512)).thenReturn(range);

        TrackAudioRange result = useCase.execute(new GetTrackAudioCommand(trackId, 1024, 512));

        assertEquals(1024, result.offset());
        assertEquals(512, result.length());
    }
}