package com.spotify.track.infrastructure.persistence.mapper;

import com.spotify.track.domain.entity.Track;
import com.spotify.track.infrastructure.persistence.entity.TrackJpaEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrackJpaMapperTest {

    private final TrackJpaMapper mapper = Mappers.getMapper(TrackJpaMapper.class);

    @Test
    void should_MapDomainToJpa_andBack_roundTrip() {
        Track domain = Track.builder()
                .id(java.util.UUID.randomUUID())
                .title("Blinding Lights")
                .artist("The Weeknd")
                .album("After Hours")
                .durationMs(200_000L)
                .artworkUrl("https://artwork.png")
                .audioUrl("https://audio.mp3")
                .build();

        TrackJpaEntity jpa = mapper.toJpaEntity(domain);
        Track back = mapper.toDomainEntity(jpa);

        assertEquals(domain.getId(), back.getId());
        assertEquals("Blinding Lights", back.getTitle());
        assertEquals(200_000L, back.getDurationMs());
        assertEquals("https://audio.mp3", back.getAudioUrl());
    }

    @Test
    void should_MapNullAudioUrl_andTextFields_when_OptionalFieldsEmpty() {
        Track domain = Track.builder()
                .id(java.util.UUID.randomUUID())
                .title("T")
                .artist("A")
                .durationMs(1L)
                .build(); // album/artworkUrl/audioUrl null

        Track back = mapper.toDomainEntity(mapper.toJpaEntity(domain));

        assertNull(back.getAlbum());
        assertNull(back.getArtworkUrl());
        assertNull(back.getAudioUrl());
    }
}