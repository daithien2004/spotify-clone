package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.UploadTrackAudioCommand;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.event.TrackAudioUploaded;
import com.spotify.track.domain.exception.TrackNotFoundException;
import com.spotify.track.domain.repository.DomainEventPublisher;
import com.spotify.track.domain.repository.TrackAudioRepository;
import com.spotify.track.domain.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadTrackAudioUseCaseImplTest {

    @Mock
    private TrackRepository trackRepository;
    @Mock
    private TrackAudioRepository trackAudioRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private UploadTrackAudioUseCaseImpl useCase;

    @Test
    void should_ThrowTrackNotFound_when_TrackMissing() {
        UUID trackId = UUID.randomUUID();
        when(trackRepository.findById(trackId)).thenReturn(Optional.empty());

        assertThrows(TrackNotFoundException.class,
                () -> useCase.execute(command(trackId, 1024, "audio/mpeg")));
    }

    @Test
    void should_ThrowIllegalArgument_when_FileIsEmpty() {
        UUID trackId = UUID.randomUUID();
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(trackOf(trackId)));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(command(trackId, 0, "audio/mpeg")));
    }

    @Test
    void should_ThrowIllegalArgument_when_FileExceedsLimit() {
        UUID trackId = UUID.randomUUID();
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(trackOf(trackId)));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(command(trackId, UploadTrackAudioUseCase.MAX_AUDIO_BYTES + 1, "audio/mpeg")));
    }

    @Test
    void should_StoreAudioAndUpdateTrack_when_FileValid() {
        UUID trackId = UUID.randomUUID();
        Track track = trackOf(trackId);
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
        when(trackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command(trackId, 2048, "audio/mpeg"));

        ArgumentCaptor<Track> saved = ArgumentCaptor.forClass(Track.class);
        verify(trackRepository).save(saved.capture());
        // audioUrl now points at the streaming endpoint
        assertTrue(saved.getValue().getAudioUrl().contains(trackId.toString()));
        verify(trackAudioRepository).putAudio(any(), any(), anyLong(), any());
        verify(eventPublisher).publish(any(TrackAudioUploaded.class));
    }

    private Track trackOf(UUID id) {
        return Track.builder().id(id).title("Song").artist("Artist").build();
    }

    private UploadTrackAudioCommand command(UUID trackId, long size, String contentType) {
        InputStream content = new ByteArrayInputStream(new byte[(int) size]);
        return new UploadTrackAudioCommand(trackId, content, size, contentType);
    }
}