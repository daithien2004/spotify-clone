package com.spotify.track.infrastructure.integration;

import com.spotify.track.infrastructure.persistence.entity.TrackJpaEntity;
import com.spotify.track.infrastructure.persistence.repository.JpaTrackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataJpaTest cho track — verify Flyway V1→V2 mapping + batch findAllById (thứ tự
 * không đảm bảo) chạy đúng trên Postgres thật, không sai constraint.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TrackJpaRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JpaTrackRepository repo;

    private TrackJpaEntity createTrack(String title) {
        TrackJpaEntity t = new TrackJpaEntity();
        t.setTitle(title);
        t.setArtist("Artist");
        t.setAlbum("Album");
        t.setDurationMs(200_000L);
        t.setArtworkUrl("/figma/art.png");
        t.setAudioUrl("/api/v1/tracks/x/audio");
        return t;
    }

    @Test
    @DisplayName("save + findAllById batch trả đủ track đã lưu")
    void should_SaveAndBatchFind_when_TracksCreated() {
        TrackJpaEntity a = repo.save(createTrack("Track A"));
        TrackJpaEntity b = repo.save(createTrack("Track B"));
        repo.flush();

        List<TrackJpaEntity> found = repo.findAllById(List.of(a.getId(), b.getId()));

        assertThat(found).hasSize(2);
        assertThat(found).extracting(TrackJpaEntity::getTitle)
                .containsExactlyInAnyOrder("Track A", "Track B");
    }

    @Test
    @DisplayName("keyword → duration_ms NOT NULL, audio_url persist đúng")
    void should_PersistTrackFields_when_Saved() {
        TrackJpaEntity t = createTrack("Free Spirit");
        repo.save(t);
        repo.flush();

        TrackJpaEntity found = repo.findById(t.getId()).orElseThrow();
        assertThat(found.getArtist()).isEqualTo("Artist");
        assertThat(found.getDurationMs()).isEqualTo(200_000L);
        assertThat(found.getAudioUrl()).isEqualTo("/api/v1/tracks/x/audio");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }
}
