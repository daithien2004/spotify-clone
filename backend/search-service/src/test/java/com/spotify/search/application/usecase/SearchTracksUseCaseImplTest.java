package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.SearchTracksCommand;
import com.spotify.search.application.dto.SearchTrackItem;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchTracksUseCaseImplTest {

    @Mock
    private TrackSearchRepository repository;

    @InjectMocks
    private SearchTracksUseCaseImpl useCase;

    private final TrackSearchDocument khalid = new TrackSearchDocument(UUID.randomUUID(),
            "Free Spirit", "Khalid", "Free Spirit (Explicit)", 182_000L,
            "https://artwork.png", null);

    @Test
    void should_ReturnMappedItems_when_MatchByTitle() {
        when(repository.search("free spirit", 10)).thenReturn(List.of(khalid));

        List<SearchTrackItem> result = useCase.execute(new SearchTracksCommand("  Free Spirit ", 10));

        assertEquals(1, result.size());
        assertEquals("Free Spirit", result.get(0).title());
        assertEquals("Khalid", result.get(0).artist());
    }

    @Test
    void should_ClampLimit_to50() {
        when(repository.search("free", 50)).thenReturn(List.of(khalid));

        List<SearchTrackItem> result = useCase.execute(new SearchTracksCommand("free", 999));

        assertEquals(1, result.size());
    }

    @Test
    void should_FlattenLimit_to1() {
        when(repository.search("free", 1)).thenReturn(List.of());

        List<SearchTrackItem> result = useCase.execute(new SearchTracksCommand("free", 0));

        assertEquals(0, result.size());
    }

    @Test
    void should_Throw_when_QueryBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new SearchTracksCommand("   ", 10)));
        verifyNoInteractions(repository);
    }
}