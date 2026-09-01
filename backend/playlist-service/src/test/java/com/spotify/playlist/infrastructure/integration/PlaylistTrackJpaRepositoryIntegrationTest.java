package com.spotify.playlist.infrastructure.integration;

import com.spotify.playlist.infrastructure.persistence.entity.PlaylistTrackJpaEntity;
import com.spotify.playlist.infrastructure.persistence.repository.JpaPlaylistTrackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataJpaTest cho playlist — verify Flyway V1→V3 mapping + 2 custom query quan trọng
 * (sắp xếp LexoRank + query rebalance) chạy đúng trên Postgres thật.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlaylistTrackJpaRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JpaPlaylistTrackRepository repo;

    private UUID playlistId = UUID.randomUUID();

    private PlaylistTrackJpaEntity addTrack(String lexoRank) {
        PlaylistTrackJpaEntity e = new PlaylistTrackJpaEntity();
        e.setPlaylistId(playlistId);
        e.setTrackId(UUID.randomUUID());
        e.setLexoRank(lexoRank);
        return repo.save(e);
    }

    @Test
    @DisplayName("findByPlaylistIdOrderByLexoRankAsc trả track theo đúng thứ tự LexoRank")
    void should_ReturnTracksOrderedByLexoRank_when_Saved() {
        // Thêm lộn xộn — query phải sort theo lexo_rank ASC (LexoRank so sánh chuỗi)
        addTrack("c");
        addTrack("a");
        addTrack("b");

        List<PlaylistTrackJpaEntity> tracks = repo.findByPlaylistIdOrderByLexoRankAsc(playlistId);

        assertThat(tracks).hasSize(3);
        assertThat(tracks.get(0).getLexoRank()).isEqualTo("a");
        assertThat(tracks.get(1).getLexoRank()).isEqualTo("b");
        assertThat(tracks.get(2).getLexoRank()).isEqualTo("c");
    }

    @Test
    @DisplayName("findPlaylistIdsNeedingRebalance chỉ trả playlist có lexo_rank vượt minLength")
    void should_ReturnOnlyLongLexoRankPlaylists_when_RebalanceQuery() {
        // Playlist A: rank ngắn ("0","1") — không cần rebalance
        UUID shortPlaylist = UUID.randomUUID();
        PlaylistTrackJpaEntity a1 = new PlaylistTrackJpaEntity();
        a1.setPlaylistId(shortPlaylist); a1.setTrackId(UUID.randomUUID()); a1.setLexoRank("0");
        PlaylistTrackJpaEntity a2 = new PlaylistTrackJpaEntity();
        a2.setPlaylistId(shortPlaylist); a2.setTrackId(UUID.randomUUID()); a2.setLexoRank("1");
        repo.save(a1); repo.save(a2);

        // Playlist B: rank dài "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" (28 ký tự) — cần rebalance
        PlaylistTrackJpaEntity b1 = new PlaylistTrackJpaEntity();
        b1.setPlaylistId(playlistId); b1.setTrackId(UUID.randomUUID()); b1.setLexoRank("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        repo.save(b1);

        // minLength 20: chỉ playlist B (28>20) được trả, playlist A (1) không
        List<UUID> needing = repo.findPlaylistIdsNeedingRebalance(20);

        assertThat(needing).contains(playlistId).doesNotContain(shortPlaylist);
    }
}
