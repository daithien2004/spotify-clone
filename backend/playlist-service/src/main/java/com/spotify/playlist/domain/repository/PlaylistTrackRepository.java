package com.spotify.playlist.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.spotify.playlist.domain.entity.PlaylistTrack;

public interface PlaylistTrackRepository {
    Optional<PlaylistTrack> findById(UUID id);

    List<PlaylistTrack> findAllByPlaylistId(UUID playlistId);

    /** Playlists with any rank longer than {@code minRankLength} — candidates for rebalance. */
    List<UUID> findPlaylistIdsNeedingRebalance(int minRankLength);

    void save(PlaylistTrack playlistTrack);

    void saveAll(List<PlaylistTrack> playlistTracks);
}
