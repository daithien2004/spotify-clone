package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.IndexTrackCommand;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IndexTrackUseCaseImplTest {

    @Mock
    private TrackSearchRepository repository;

    @InjectMocks
    private IndexTrackUseCaseImpl useCase;

    @Test
    void should_IndexDocument_when_Valid() {
        TrackSearchDocument doc = new TrackSearchDocument(UUID.randomUUID(),
                "Free Spirit", "Khalid", "Free Spirit (Explicit)", 182_000L,
                "https://artwork.png", null);

        useCase.execute(new IndexTrackCommand(doc));

        verify(repository).index(doc);
    }

    @Test
    void should_Reindex_when_SameTrackAgain() {
        TrackSearchDocument doc = new TrackSearchDocument(UUID.randomUUID(),
                "Free Spirit", "Khalid", null, 182_000L, null, null);

        useCase.execute(new IndexTrackCommand(doc));
        useCase.execute(new IndexTrackCommand(doc));

        // Re-indexing the same doc is allowed (idempotent upsert), so it lands twice.
        verify(repository, times(2)).index(doc);
    }

    @Test
    void should_Throw_when_DocumentMissing() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new IndexTrackCommand(null)));
        verifyNoInteractions(repository);
    }

    @Test
    void should_Throw_when_IdMissing() {
        TrackSearchDocument doc = new TrackSearchDocument(null,
                "Free Spirit", "Khalid", null, 182_000L, null, null);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new IndexTrackCommand(doc)));
        verifyNoInteractions(repository);
    }
}