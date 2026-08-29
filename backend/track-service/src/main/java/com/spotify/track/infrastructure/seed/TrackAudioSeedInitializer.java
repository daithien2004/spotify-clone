package com.spotify.track.infrastructure.seed;

import com.spotify.track.domain.exception.TrackAudioNotFoundException;
import com.spotify.track.domain.repository.TrackAudioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

/**
 * Dev convenience: on boot, makes sure every seeded track (V2 seed migration)
 * has a playable WAV object in MinIO. Idempotent — re-checks existence, and
 * degrades to a warning when MinIO isn't running so the service still boots.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackAudioSeedInitializer implements ApplicationRunner {

    private static final List<UUID> SEEDED_TRACK_IDS = List.of(
            UUID.fromString("20000000-0000-4000-8000-000000000001"),
            UUID.fromString("20000000-0000-4000-8000-000000000002"),
            UUID.fromString("20000000-0000-4000-8000-000000000003"),
            UUID.fromString("20000000-0000-4000-8000-000000000004"),
            UUID.fromString("20000000-0000-4000-8000-000000000005"),
            UUID.fromString("20000000-0000-4000-8000-000000000006"));

    private static final int LED_SECONDS = 15;

    private final TrackAudioRepository trackAudioRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (int i = 0; i < SEEDED_TRACK_IDS.size(); i++) {
            UUID trackId = SEEDED_TRACK_IDS.get(i);
            if (audioExists(trackId)) {
                continue;
            }
            try {
                byte[] wav = WavSynthesizer.melody(i, LED_SECONDS);
                trackAudioRepository.putAudio(trackId, new ByteArrayInputStream(wav), wav.length, "audio/wav");
                log.info("Seeded audio for track {}", trackId);
            } catch (Exception e) {
                log.warn("Audio seed skipped for track {}: {}", trackId, e.getMessage());
            }
        }
    }

    private boolean audioExists(UUID trackId) {
        try {
            trackAudioRepository.getAudio(trackId, 0, 1);
            return true;
        } catch (TrackAudioNotFoundException e) {
            return false;
        }
    }
}