package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.RemoveTrackCommand;
import com.spotify.search.domain.repository.TrackSearchRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RemoveTrackUseCaseImplTest {

    @Mock
    private TrackSearchRepository repository;

    @InjectMocks
    private RemoveTrackUseCaseImpl useCase;

    @Test
    void should_RemoveTrack_when_Valid() {
        UUID id = UUID.randomUUID();

        useCase.execute(new RemoveTrackCommand(id));

        verify(repository).remove(id);
    }

    @Test
    void should_Throw_when_TrackIdMissing() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new RemoveTrackCommand(null)));
        verifyNoInteractions(repository);
    }
}