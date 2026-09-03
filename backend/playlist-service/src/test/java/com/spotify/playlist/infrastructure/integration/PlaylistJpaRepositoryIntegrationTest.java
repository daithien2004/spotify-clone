package com.spotify.playlist.infrastructure.integration;

import com.spotify.playlist.domain.entity.Playlist;
import com.spotify.playlist.infrastructure.persistence.mapper.PlaylistJpaMapper;
import com.spotify.playlist.infrastructure.persistence.mapper.PlaylistJpaMapperImpl;
import com.spotify.playlist.infrastructure.persistence.repository.JpaPlaylistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verify PlaylistJpaMapper + JpaPlaylistRepository.save round-trip on real Postgres:
 * data persisted with its own id (not auto-generated) and readable back via findById.
 * Supports the CreatePlaylist use case which relies on repository.save.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// Import MapStruct-generated impl (interface itself isn't a bean); the real app gets
// it via component scan from maven-compiler-plugin annotation processing.
@Import(PlaylistJpaMapperImpl.class)
class PlaylistJpaRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JpaPlaylistRepository repo;

    @Autowired
    private PlaylistJpaMapper mapper;

    @Test
    @DisplayName("save + findById round-trip giữ nguyên mọi field (id chỉ định sẵn)")
    void should_RoundTripSavedPlaylist_when_UsingProvidedId() {
        UUID id = UUID.randomUUID();
        Playlist domain = Playlist.builder()
                .id(id)
                .title("My Mix")
                .description("hand-picked")
                .ownerName("Alice")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Identity is owned by the application layer (use case calls UUID.randomUUID()),
        // so the JPA entity must persist the provided id verbatim — Hibernate 6's
        // GenerationType.UUID would otherwise regenerate a different id on save.
        Playlist saved = mapper.toDomainEntity(repo.save(mapper.toJpaEntity(domain)));
        Playlist fetched = mapper.toDomainEntity(repo.findById(id).orElseThrow());

        assertThat(fetched.getId()).isEqualTo(id);
        assertThat(fetched.getTitle()).isEqualTo("My Mix");
        assertThat(fetched.getDescription()).isEqualTo("hand-picked");
        assertThat(fetched.getOwnerName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("save không cover_url khi null — column nullable, JPA không ép")
    void should_SavePlaylistWithoutCoverUrl_when_Null() {
        Playlist domain = Playlist.builder()
                .id(UUID.randomUUID())
                .title("Coverless")
                .ownerName("You")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Playlist saved = mapper.toDomainEntity(repo.save(mapper.toJpaEntity(domain)));

        assertThat(saved.getCoverUrl()).isNull();
        assertThat(saved.getTitle()).isEqualTo("Coverless");
    }
}
