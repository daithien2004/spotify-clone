package com.spotify.track.infrastructure.persistence.adapter;

import com.spotify.track.domain.entity.TrackAudioRange;
import com.spotify.track.domain.exception.TrackAudioNotFoundException;
import com.spotify.track.domain.repository.TrackAudioRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;

/**
 * MinIO-backed audio storage. Object key is {@code {trackId}/audio}; the
 * storage bucket "tracks" is created on first boot if absent.
 */
@Slf4j
@Component
public class MinioTrackAudioRepository implements TrackAudioRepository {

    private static final String OBJECT_PREFIX = "/audio";

    private final MinioClient minioClient;
    private final String bucket; // resolved from configuration

    public MinioTrackAudioRepository(MinioClient minioClient,
                                     com.spotify.track.infrastructure.config.MinioProperties properties) {
        this.minioClient = minioClient;
        this.bucket = properties.bucket();
    }

    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket '{}'", bucket);
            }
        } catch (Exception e) {
            // Storage unavailable at boot (dev without MinIO) — don't crash the service
            log.warn("MinIO bucket '{}' not ready yet: {}", bucket, e.getMessage());
        }
    }

    @Override
    public void putAudio(UUID trackId, InputStream content, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key(trackId))
                    .stream(content, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store audio for track " + trackId, e);
        }
    }

    @Override
    public TrackAudioRange getAudio(UUID trackId, long offset, long length) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(key(trackId)).build());
            long totalSize = stat.size();

            long effectiveLength = Math.min(length, Math.max(0, totalSize - offset));
            InputStream content = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key(trackId))
                    .offset(offset)
                    .length(effectiveLength)
                    .build());

            String contentType = stat.contentType() != null ? stat.contentType() : "audio/mpeg";
            return new TrackAudioRange(content, offset, effectiveLength, totalSize, contentType);
        } catch (ErrorResponseException e) {
            throw new TrackAudioNotFoundException(trackId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to stream audio for track " + trackId, e);
        }
    }

    @Override
    public void deleteAudio(UUID trackId) {
        try {
            minioClient.removeObject(io.minio.RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key(trackId))
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete audio for track " + trackId, e);
        }
    }

    private String key(UUID trackId) {
        return trackId + OBJECT_PREFIX;
    }
}