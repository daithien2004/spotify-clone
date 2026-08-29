# Search-service (Elasticsearch) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the new `search-service` (port 8086) that indexes tracks into Elasticsearch via Kafka events + startup bootstrap, exposing `GET /api/v1/search/tracks?q=&limit=` behind the gateway, and wire the FE SearchBar to it (replacing the `TRACK_INDEX` mock).

**Architecture:** Clean Architecture following the track-service pattern. cross-service contract via a shared `TrackEventEnvelope` record in `common-lib`. track-service publishes track events to Kafka topic `spotify.track.events`; search-service consumes them into Elasticsearch index `tracks` (multi-match title^3/artist^2/album, fuzziness AUTO) and bootstraps history once at startup from `GET /api/v1/tracks` (list-all). Search is exposed through gateway `/api/v1/search/**` → 8086 with the existing JWT filter.

**Tech Stack:** Java 21, Spring Boot 3.2.4, spring-kafka (JsonSerializer/JsonDeserializer), `spring-boot-starter-data-elasticsearch` (`co.elastic.clients` ElasticsearchClient), Elasticsearch 8.x (docker single-node), common-lib (ApiResponse, ServiceSecurityConfig, GlobalResponseWrapper), Next.js 14 + React Query + Zustand.

**Spec:** `docs/superpowers/specs/2026-08-29-search-service-design.md`

## Global Constraints

- Maven on Windows: `./mvnw` is broken (ClassNotFoundException). Use `/d/_mvn_tool/apache-maven-3.9.12/bin/mvn` from `backend/` for every build/test. Example: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -am -DskipTests=false`
- Full backend gate at the end: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test`
- Commit convention: `{type}({scope}): {description}` (`feat(track)`, `feat(search)`, `chore(deps)`).
- Clean Architecture: `domain/` never imports Spring/JPA/Kafka. `presentation → application → domain`, `infrastructure → domain`.
- Domain purity: no Lombok records needed in `domain/`; domain records are plain Java.
- Common-lib is shared by every service: the shared Kafka contract (`TrackEventEnvelope`) lives there.
- No wildcard imports; 2-space indent; < 40-line methods; early returns.
- `spring.kafka.enabled` is a custom app flag (same pattern as auth-service `KafkaSecurityAuditPublisher`), default from `${KAFKA_ENABLED:true}`.
- port 8086 must be free: **kafka-ui currently binds 8086 → change to 8087** in docker-compose (spec §3 docker-compose change, required by the port plan).
- Elasticsearch: single-node, `xpack.security.enabled=false`, port 9200, `ES_JAVA_OPTS=-Xms512m -Xmx512m`, volume `es_data`.
- ES index `tracks` mapping: `id=keyword`, `title/artist/album=text`, `artworkUrl/audioUrl=keyword(index:false)`, `durationMs=long`. Search default limit 10, cap 50.
- Track-service dev security: `GET /api/v1/tracks` (list-all) must be reachable by the search-service bootstrap **without** a JWT → add an `@Order(1)` permit chain in track-service that matches only `GET /api/v1/tracks` (POST create stays authenticated via the common `ServiceSecurityConfig`).
- FE: React Query for server state, granular Zustand selectors, tokens-only styling (no arbitrary hex), centralized query keys in `queryKeys.ts`.

---

### Task 1: common-lib — shared `TrackEventEnvelope` Kafka contract

**Files:**
- Create: `backend/common-lib/src/main/java/com/spotify/common/event/TrackEventType.java`
- Create: `backend/common-lib/src/main/java/com/spotify/common/event/TrackEventEnvelope.java`
- Test: `backend/common-lib/src/test/java/com/spotify/common/event/TrackEventEnvelopeTest.java`

**Interfaces:**
- Consumes: nothing (new shared contract).
- Produces: `TrackEventEnvelope(TrackEventType eventType, String eventId, String occurredOn, TrackPayload track)` — a Java record; nested `record TrackPayload(String id, String title, String artist, String album, Long durationMs, String artworkUrl, String audioUrl)`; enum `TrackEventType { TRACK_UPLOADED, TRACK_UPDATED, TRACK_REMOVED, TRACK_AUDIO_UPLOADED }`. Later tasks deserialize this from JSON (`jackson-databind` handles records natively).

- [ ] **Step 1: Write the failing test**

```java
package com.spotify.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The Kafka envelope must survive a Jackson round-trip (producer JsonSerializer ↔ consumer JsonDeserializer). */
class TrackEventEnvelopeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_SerializeAndDeserialize_when_EnvelopeHasFullPayload() throws Exception {
        TrackEventEnvelope envelope = new TrackEventEnvelope(
                TrackEventType.TRACK_UPLOADED,
                "12e0c789-0b18-46cc-9a4a-b106e5bf1f1e",
                "2026-08-29T10:15:30",
                new TrackEventEnvelope.TrackPayload(
                        "9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b",
                        "Free Spirit", "Khalid", "Free Spirit (Explicit)",
                        182_000L, "https://artwork/free-spirit.png", "https://audio/free-spirit.mp3"));

        String json = objectMapper.writeValueAsString(envelope);
        TrackEventEnvelope back = objectMapper.readValue(json, TrackEventEnvelope.class);

        assertEquals(TrackEventType.TRACK_UPLOADED, back.eventType());
        assertNotNull(back.track());
        assertEquals("Free Spirit", back.track().title());
        assertEquals(182_000L, back.track().durationMs());
    }

    @Test
    void should_DeserializeAudioUploadEnvelope_when_OnlyIdPresent() throws Exception {
        String json = """
                {"eventType":"TRACK_AUDIO_UPLOADED","eventId":"uuid-1","occurredOn":"2026-08-29T10:15:30",
                 "track":{"id":"9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b"}}

                """;

        TrackEventEnvelope envelope = new ObjectMapper().readValue(json, TrackEventEnvelope.class);

        assertEquals(TrackEventType.TRACK_AUDIO_UPLOADED, envelope.eventType());
        assertEquals("9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b", envelope.track().id());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl common-lib -Dtest=TrackEventEnvelopeTest`
Expected: FAIL — cannot find symbol `TrackEventEnvelope`.

- [ ] **Step 3: Write the minimal implementation**

```java
package com.spotify.common.event;

/** Event types carried on the shared track-events topic (domain.md event map). */
public enum TrackEventType {
    TRACK_UPLOADED,
    TRACK_UPDATED,
    TRACK_REMOVED,
    TRACK_AUDIO_UPLOADED
}
```

```java
package com.spotify.common.event;

/**
 * Cross-service Kafka contract (track-service producer → search-service consumer), topic
 * {@code spotify.track.events}. Plain record — serialized with Jackson's record support;
 * {@code occurredOn} is ISO-8601 text so both sides need no JSR-310 module tweaks.
 */
public record TrackEventEnvelope(
        TrackEventType eventType,
        String eventId,
        String occurredOn,
        TrackPayload track
) {
    /** Snapshot of the track aggregate carried by the event. */
    public record TrackPayload(
            String id,
            String title,
            String artist,
            String album,
            Long durationMs,
            String artworkUrl,
            String audioUrl
    ) {}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl common-lib -Dtest=TrackEventEnvelopeTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Full common-lib gate**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl common-lib`
Expected: BUILD SUCCESS (5 existing + 2 new).

- [ ] **Step 6: Commit**

```bash
git add backend/common-lib/src/main/java/com/spotify/common/event/ backend/common-lib/src/test/java/com/spotify/common/event/
git commit -m "feat(common): add shared TrackEventEnvelope Kafka contract"
```

---

### Task 2: track-service — full-payload `TrackUploaded` + new `TrackUpdated` events

**Files:**
- Modify: `backend/track-service/src/main/java/com/spotify/track/domain/event/TrackUploaded.java` (replace)
- Create: `backend/track-service/src/main/java/com/spotify/track/domain/event/TrackUpdated.java`
- Modify: `backend/track-service/src/main/java/com/spotify/track/application/usecase/CreateTrackUseCaseImpl.java:38-39`
- Modify: `backend/track-service/src/main/java/com/spotify/track/application/usecase/UpdateTrackUseCaseImpl.java`
- Test: `backend/track-service/src/test/java/com/spotify/track/application/usecase/CreateTrackUseCaseImplTest.java` (extend assertions)

**Interfaces:**
- Consumes: `Track` entity (`com.spotify.track.domain.entity.Track`) — getters `getId/getTitle/getArtist/getAlbum/getDurationMs/getArtworkUrl/getAudioUrl`; the `TrackUploaded`/`TrackUpdated` events extend `DomainEvent` (auto `eventId` + `occurredOn`).
- Produces: `TrackUploaded(UUID trackId, String title, String artist, String album, Long durationMs, String artworkUrl, String audioUrl)` and `TrackUpdated(/* same 7 params */)` — used by `TrackKafkaDomainEventPublisher` (Task 3) and the search-service consumer (Task 12).

- [ ] **Step 1: Write the failing test (extend the existing publish assertion to require the full payload)**

Replace the captor block in `should_CreateTrack_and_PublishEvent_when_RequestIsValid` (currently lines 46-49) with:

```java
        ArgumentCaptor<TrackUploaded> eventCaptor = ArgumentCaptor.forClass(TrackUploaded.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        TrackUploaded event = eventCaptor.getValue();
        assertEquals(result.getId(), event.getTrackId());
        assertEquals("The Weeknd", event.getArtist());
        assertEquals("After Hours", event.getAlbum());
        assertEquals(200_000L, event.getDurationMs());
        assertEquals("https://artwork/blinding-lights.png", event.getArtworkUrl());
        assertEquals(null, event.getAudioUrl());
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl track-service -Dtest=CreateTrackUseCaseImplTest`
Expected: FAIL — `getAlbum()`, `getDurationMs()`, `getArtworkUrl()`, `getAudioUrl()` do not exist on `TrackUploaded`.

- [ ] **Step 3: Replace `TrackUploaded` and add `TrackUpdated`**

Replace `backend/track-service/src/main/java/com/spotify/track/domain/event/TrackUploaded.java`:

```java
package com.spotify.track.domain.event;

import java.util.UUID;

/** Track.Uploaded — emitted when a track enters the catalog (domain.md event map). */
public class TrackUploaded extends DomainEvent {
    private final UUID trackId;
    private final String title;
    private final String artist;
    private final String album;
    private final Long durationMs;
    private final String artworkUrl;
    private final String audioUrl;

    public TrackUploaded(UUID trackId, String title, String artist, String album,
                         Long durationMs, String artworkUrl, String audioUrl) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationMs = durationMs;
        this.artworkUrl = artworkUrl;
        this.audioUrl = audioUrl;
    }

    public UUID getTrackId() {
        return trackId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }
}
```

Create `backend/track-service/src/main/java/com/spotify/track/domain/event/TrackUpdated.java`:

```java
package com.spotify.track.domain.event;

import java.util.UUID;

/** Track.Updated — emitted when catalog metadata changes (domain.md event map). */
public class TrackUpdated extends DomainEvent {
    private final UUID trackId;
    private final String title;
    private final String artist;
    private final String album;
    private final Long durationMs;
    private final String artworkUrl;
    private final String audioUrl;

    public TrackUpdated(UUID trackId, String title, String artist, String album,
                        Long durationMs, String artworkUrl, String audioUrl) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationMs = durationMs;
        this.artworkUrl = artworkUrl;
        this.audioUrl = audioUrl;
    }

    public UUID getTrackId() {
        return trackId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }
}
```

- [ ] **Step 4: Update `CreateTrackUseCaseImpl` to publish the full payload**

Replace line 39:

```java
        domainEventPublisher.publish(new TrackUploaded(saved.getId(), saved.getTitle(), saved.getArtist()));
```

with:

```java
        domainEventPublisher.publish(new TrackUploaded(
                saved.getId(), saved.getTitle(), saved.getArtist(), saved.getAlbum(),
                saved.getDurationMs(), saved.getArtworkUrl(), saved.getAudioUrl()));
```

- [ ] **Step 5: Update `UpdateTrackUseCaseImpl` to publish `TrackUpdated`**

Add the publisher dependency and publish after save. Replace the class fields (lines 19-21) with:

```java
    private final TrackRepository trackRepository;
    private final DomainEventPublisher domainEventPublisher;
```

Replace the body (lines 24-38) with:

```java
    @Override
    @Transactional
    public TrackResponse execute(UUID trackId, CreateTrackRequest request) {
        Track existing = trackRepository.findById(trackId)
                .orElseThrow(() -> new TrackNotFoundException(trackId));

        validate(request);

        Track updated = existing.withUpdatedMetadata(
                request.title(),
                request.artist(),
                request.album(),
                request.durationMs(),
                request.artworkUrl());

        Track saved = trackRepository.save(updated);
        domainEventPublisher.publish(new TrackUpdated(
                saved.getId(), saved.getTitle(), saved.getArtist(), saved.getAlbum(),
                saved.getDurationMs(), saved.getArtworkUrl(), saved.getAudioUrl()));
        return TrackResponse.from(saved);
    }
```

- [ ] **Step 6: Run track-service tests to verify green**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl track-service`
Expected: BUILD SUCCESS (Create 5 updated + Update 3 + others unchanged = 19).

- [ ] **Step 7: Commit**

```bash
git add backend/track-service/src/main/java/com/spotify/track/domain/event/ backend/track-service/src/main/java/com/spotify/track/application/usecase/ backend/track-service/src/test/java/com/spotify/track/application/usecase/
git commit -m "feat(track): carry full track payload in Uploaded/Updated domain events"
```

---

### Task 3: track-service — real Kafka domain-event publisher

**Files:**
- Modify: `backend/track-service/pom.xml` (add spring-kafka)
- Modify: `backend/track-service/src/main/resources/application.yml` (kafka producer config)
- Create: `backend/track-service/src/main/java/com/spotify/track/infrastructure/event/TrackEventEnvelopeMapper.java`
- Create: `backend/track-service/src/main/java/com/spotify/track/infrastructure/event/TrackKafkaDomainEventPublisher.java`
- Delete: `backend/track-service/src/main/java/com/spotify/track/infrastructure/event/TrackLogDomainEventPublisher.java`
- Test: `backend/track-service/src/test/java/com/spotify/track/infrastructure/event/TrackKafkaDomainEventPublisherTest.java`
- Test: `backend/track-service/src/test/java/com/spotify/track/infrastructure/event/TrackEventEnvelopeMapperTest.java`

**Interfaces:**
- Consumes: `TrackUploaded`/`TrackUpdated`/`TrackAudioUploaded` (Task 2 shapes), `TrackEventEnvelope` (common-lib, Task 1), `DomainEventPublisher` (existing port), spring-kafka `KafkaTemplate<String, Object>`.
- Produces: `TrackKafkaDomainEventPublisher implements DomainEventPublisher` — logs like the old publisher, then sends `TrackEventEnvelope` to topic `spotify.track.events` when `spring.kafka.enabled` is true. Mapper: `TrackEventEnvelope toEnvelope(DomainEvent event)` — `TRACK_UPLOADED`/`TRACK_UPDATED` map the 7 fields, `TRACK_AUDIO_UPLOADED` maps only `track.id` (consumer ignores it, spec §3).

- [ ] **Step 1: Write the failing test (publisher)**

```java
package com.spotify.track.infrastructure.event;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.track.domain.event.TrackUploaded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackKafkaDomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private TrackEventEnvelopeMapper envelopeMapper;

    private TrackKafkaDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        // Explicit ctor pattern (portable, no reflection): the @Value flag is passed in.
    }

    private TrackUploaded anyUploaded() {
        return new TrackUploaded(UUID.randomUUID(), "Free Spirit", "Khalid", "Free Spirit (Explicit)",
                182_000L, "https://artwork.png", null);
    }

    @Test
    void should_SendEnvelope_when_KafkaEnabled() {
        TrackEventEnvelope envelope = new TrackEventEnvelope(
                com.spotify.common.event.TrackEventType.TRACK_UPLOADED, "evt-1", "2026-08-29T10:15:30",
                new TrackEventEnvelope.TrackPayload("id-1", "Free Spirit", "Khalid",
                        "Free Spirit (Explicit)", 182_000L, "https://artwork.png", null));
        when(envelopeMapper.toEnvelope(any())).thenReturn(envelope);
        when(kafkaTemplate.send(anyString(), anyString(), any(TrackEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher = new TrackKafkaDomainEventPublisher(kafkaTemplate, envelopeMapper, true);

        publisher.publish(anyUploaded());

        verify(kafkaTemplate).send(eq(TrackKafkaDomainEventPublisher.TRACK_EVENTS_TOPIC),
                eq("evt-1"), eq(envelope));
    }

    @Test
    void should_NotSend_when_KafkaDisabled() {
        publisher = new TrackKafkaDomainEventPublisher(kafkaTemplate, envelopeMapper, false);

        publisher.publish(anyUploaded());

        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }
}
```

- [ ] **Step 2: Write the failing test (mapper)**

```java
package com.spotify.track.infrastructure.event;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.common.event.TrackEventType;
import com.spotify.track.domain.event.TrackAudioUploaded;
import com.spotify.track.domain.event.TrackUpdated;
import com.spotify.track.domain.event.TrackUploaded;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackEventEnvelopeMapperTest {

    private final TrackEventEnvelopeMapper mapper = new TrackEventEnvelopeMapper();

    @Test
    void should_MapUploaded_toEnvelope() {
        UUID id = UUID.randomUUID();
        TrackUploaded event = new TrackUploaded(id, "Free Spirit", "Khalid", "Free Spirit (Explicit)",
                182_000L, "https://artwork.png", null);

        TrackEventEnvelope e = mapper.toEnvelope(event);

        assertEquals(TrackEventType.TRACK_UPLOADED, e.eventType());
        assertEquals(id.toString(), e.track().id());
        assertEquals("Khalid", e.track().artist());
        assertEquals(182_000L, e.track().durationMs());
    }

    @Test
    void should_MapUpdated_toEnvelope() {
        TrackUpdated event = new TrackUpdated(UUID.randomUUID(), "New Title", "Artist", "Album",
                100L, "", "");

        TrackEventEnvelope e = mapper.toEnvelope(event);

        assertEquals(TrackEventType.TRACK_UPDATED, e.eventType());
        assertEquals("New Title", e.track().title());
    }

    @Test
    void should_MapAudioUploaded_toEnvelopeWithOnlyId() {
        UUID id = UUID.randomUUID();
        TrackAudioUploaded event = new TrackAudioUploaded(id);

        TrackEventEnvelope e = mapper.toEnvelope(event);

        assertEquals(TrackEventType.TRACK_AUDIO_UPLOADED, e.eventType());
        assertEquals(id.toString(), e.track().id());
        assertEquals(null, e.track().title());
    }

    @Test
    void should_Throw_when_EventTypeIsUnsupported() {
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toEnvelope(new com.spotify.track.domain.event.DomainEvent() {}));
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl track-service -Dtest='TrackKafkaDomainEventPublisherTest,TrackEventEnvelopeMapperTest'`
Expected: FAIL — classes `TrackKafkaDomainEventPublisher` / `TrackEventEnvelopeMapper` not found.

- [ ] **Step 4: Add spring-kafka dependency to track-service pom**

Add inside `<dependencies>` (after the MapStruct block, before API docs):

```xml
        <!-- Kafka events -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
```

- [ ] **Step 5: Add kafka producer config to track-service application.yml**

Add under `spring:` (after the `jackson` block):

```yaml
  # ===== Kafka Configuration =====
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    enabled: ${KAFKA_ENABLED:true}
```

- [ ] **Step 6: Write the mapper**

```java
package com.spotify.track.infrastructure.event;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.common.event.TrackEventType;
import com.spotify.track.domain.event.DomainEvent;
import com.spotify.track.domain.event.TrackAudioUploaded;
import com.spotify.track.domain.event.TrackUpdated;
import com.spotify.track.domain.event.TrackUploaded;

/** Maps a track domain event onto the shared Kafka envelope (common-lib contract). */
@Component
public class TrackEventEnvelopeMapper {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Translates the domain event to the wire contract. TRACK_AUDIO_UPLOADED carries only
     * the track id — search-service consumers ignore that event type (spec §3).
     */
    public TrackEventEnvelope toEnvelope(DomainEvent event) {
        if (event instanceof TrackUploaded uploaded) {
            return envelope(TrackEventType.TRACK_UPLOADED, event,
                    uploaded.getTrackId(), uploaded.getTitle(), uploaded.getArtist(),
                    uploaded.getAlbum(), uploaded.getDurationMs(),
                    uploaded.getArtworkUrl(), uploaded.getAudioUrl());
        }
        if (event instanceof TrackUpdated updated) {
            return envelope(TrackEventType.TRACK_UPDATED, event,
                    updated.getTrackId(), updated.getTitle(), updated.getArtist(),
                    updated.getAlbum(), updated.getDurationMs(),
                    updated.getArtworkUrl(), updated.getAudioUrl());
        }
        if (event instanceof TrackAudioUploaded audio) {
            return new TrackEventEnvelope(TrackEventType.TRACK_AUDIO_UPLOADED,
                    event.getEventId().toString(), event.getOccurredOn().format(ISO),
                    new TrackEventEnvelope.TrackPayload(audio.getTrackId().toString(),
                            null, null, null, null, null, null));
        }
        throw new IllegalArgumentException("Unsupported domain event: " + event.getClass().getSimpleName());
    }

    private TrackEventEnvelope envelope(TrackEventType type, DomainEvent event,
                                        java.util.UUID trackId, String title, String artist,
                                        String album, Long durationMs, String artworkUrl, String audioUrl) {
        return new TrackEventEnvelope(type,
                event.getEventId().toString(), event.getOccurredOn().format(ISO),
                new TrackEventEnvelope.TrackPayload(trackId.toString(), title, artist, album,
                        durationMs, artworkUrl, audioUrl));
    }
}
```

- [ ] **Step 7: Write the publisher**

```java
package com.spotify.track.infrastructure.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.track.domain.event.DomainEvent;
import com.spotify.track.domain.repository.DomainEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Infrastructure adapter: publishes track domain events to Kafka (topic spotify.track.events). */
@Slf4j
@Component
public class TrackKafkaDomainEventPublisher implements DomainEventPublisher {

    public static final String TRACK_EVENTS_TOPIC = "spotify.track.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TrackEventEnvelopeMapper envelopeMapper;
    private final boolean kafkaEnabled;

    public TrackKafkaDomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                          TrackEventEnvelopeMapper envelopeMapper,
                                          @Value("${spring.kafka.enabled:true}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.envelopeMapper = envelopeMapper;
        this.kafkaEnabled = kafkaEnabled;
    }

    @Override
    public void publish(DomainEvent event) {
        // Kept the old log line — the Kafka publish is additive on top of observability.
        log.info("Domain Event Published: {} | ID: {} | At: {}",
                event.getClass().getSimpleName(), event.getEventId(), event.getOccurredOn());

        if (!kafkaEnabled) {
            return;
        }

        TrackEventEnvelope envelope = envelopeMapper.toEnvelope(event);
        kafkaTemplate.send(TRACK_EVENTS_TOPIC, envelope.eventId(), envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[TRACK_EVENT] Failed to publish {} to Kafka: {}",
                                event.getClass().getSimpleName(), ex.getMessage());
                    }
                });
    }
}
```

> Note: the explicit constructor earns the `@Value` injection while keeping the class unit-testable (the `kafkaEnabled` flag is a plain ctor arg). `@RequiredArgsConstructor` is therefore omitted.

- [ ] **Step 8: Delete the old publisher**

```bash
git rm backend/track-service/src/main/java/com/spotify/track/infrastructure/event/TrackLogDomainEventPublisher.java
```

- [ ] **Step 9: Run tests to verify green**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl track-service`
Expected: BUILD SUCCESS (19 prior + 2 publisher + 4 mapper = 25).

- [ ] **Step 10: Commit**

```bash
git add backend/track-service/pom.xml backend/track-service/src/main/resources/application.yml
git add backend/track-service/src/main/java/com/spotify/track/infrastructure/event/ backend/track-service/src/test/java/com/spotify/track/infrastructure/event/
git commit -m "feat(track): publish track events to Kafka via TrackKafkaDomainEventPublisher"
```

---

### Task 4: track-service — `GET /api/v1/tracks` list-all + unauthenticated bootstrap access

**Files:**
- Modify: `backend/track-service/src/main/java/com/spotify/track/domain/repository/TrackRepository.java`
- Modify: `backend/track-service/src/main/java/com/spotify/track/infrastructure/persistence/adapter/TrackRepositoryImpl.java`
- Create: `backend/track-service/src/main/java/com/spotify/track/application/usecase/ListTracksUseCase.java`
- Create: `backend/track-service/src/main/java/com/spotify/track/application/usecase/ListTracksUseCaseImpl.java`
- Modify: `backend/track-service/src/main/java/com/spotify/track/presentation/controller/TrackController.java`
- Create: `backend/track-service/src/main/java/com/spotify/track/infrastructure/security/TrackBootstrapApiSecurityConfig.java`
- Test: `backend/track-service/src/test/java/com/spotify/track/application/usecase/ListTracksUseCaseImplTest.java`

**Interfaces:**
- Consumes: `TrackRepository` (add `List<Track> findAll()`), `TrackResponse.from(Track)`.
- Produces: `ListTracksUseCase.execute() → List<TrackResponse>`; controller `GET /api/v1/tracks` now returns all tracks when `ids` is omitted (`@RequestParam(value="ids", required=false)`).

- [ ] **Step 1: Write the failing test**

```java
package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.TrackResponse;
import com.spotify.track.domain.entity.Track;
import com.spotify.track.domain.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTracksUseCaseImplTest {

    @Mock
    private TrackRepository trackRepository;

    @InjectMocks
    private ListTracksUseCaseImpl useCase;

    @Test
    void should_ReturnAllTracks_when_NoFilter() {
        Track a = Track.builder().id(UUID.randomUUID()).title("A").artist("X")
                .album("Ax").durationMs(1000L).artworkUrl("https://a.png").build();
        Track b = Track.builder().id(UUID.randomUUID()).title("B").artist("Y")
                .album("By").durationMs(2000L).artworkUrl("https://b.png").build();
        when(trackRepository.findAll()).thenReturn(List.of(a, b));

        List<TrackResponse> result = useCase.execute();

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).title());
        assertEquals("B", result.get(1).title());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl track-service -Dtest=ListTracksUseCaseImplTest`
Expected: FAIL — `TrackRepository.findAll()` / `ListTracksUseCaseImpl` missing.

- [ ] **Step 3: Add `findAll()` to the repository port + adapter**

Modify `TrackRepository.java` — add after `findAllByIds`:

```java
    /** Returns every track in the catalog (used by the search-service bootstrap). */
    List<Track> findAll();
```

Modify `TrackRepositoryImpl.java` — add method:

```java
    @Override
    public List<Track> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomainEntity)
                .toList();
    }
```

- [ ] **Step 4: Create the use case**

```java
package com.spotify.track.application.usecase;

import java.util.List;

import com.spotify.track.application.dto.TrackResponse;

/** Full-catalog read — feeds the search-service startup bootstrap. */
public interface ListTracksUseCase {
    List<TrackResponse> execute();
}
```

```java
package com.spotify.track.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spotify.track.application.dto.TrackResponse;
import com.spotify.track.domain.repository.TrackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListTracksUseCaseImpl implements ListTracksUseCase {

    private final TrackRepository trackRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrackResponse> execute() {
        return trackRepository.findAll().stream()
                .map(TrackResponse::from)
                .toList();
    }
}
```

- [ ] **Step 5: Update the controller — optional `ids` → list-all**

In `TrackController.java`:
- Add the import `com.spotify.track.application.usecase.ListTracksUseCase;`
- Add the field `private final ListTracksUseCase listTracksUseCase;` (constructor generated by `@RequiredArgsConstructor`).
- Replace the `getTracks` method (lines 47-51) with:

```java
    /**
     * Batch lookup for playlist joins ({@code ?ids=a,b,c} preserves order) or, when
     * {@code ids} is omitted, the full catalog — used by the search-service bootstrap.
     */
    @GetMapping
    public ResponseEntity<List<TrackResponse>> getTracks(
            @RequestParam(value = "ids", required = false) List<UUID> ids) {
        if (ids == null) {
            return ResponseEntity.ok(listTracksUseCase.execute());
        }
        return ResponseEntity.ok(getTrackByIdsUseCase.execute(ids));
    }
```

- [ ] **Step 6: Permit unauthenticated `GET /api/v1/tracks` (bootstrap) in track-service only**

Create `backend/track-service/src/main/java/com/spotify/track/infrastructure/security/TrackBootstrapApiSecurityConfig.java`:

```java
package com.spotify.track.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Public read of the full catalog at {@code GET /api/v1/tracks}: the search-service
 * bootstrap (server-to-server, no JWT) re-pulls history through this endpoint. Only the
 * exact path+method is opened — POST create and {@code /tracks/{id}*} stay behind the
 * common {@link com.spotify.common.infrastructure.security.ServiceSecurityConfig}.
 */
@Configuration
public class TrackBootstrapApiSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain bootstrapApiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(new AntPathRequestMatcher("/api/v1/tracks", HttpMethod.GET.name()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
```

- [ ] **Step 7: Run track-service tests to verify green**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl track-service`
Expected: BUILD SUCCESS (25 + 1 new = 26).

- [ ] **Step 8: Commit**

```bash
git add backend/track-service/src/main/java/com/spotify/track/domain/repository/TrackRepository.java backend/track-service/src/main/java/com/spotify/track/infrastructure/persistence/adapter/TrackRepositoryImpl.java backend/track-service/src/main/java/com/spotify/track/application/usecase/ backend/track-service/src/main/java/com/spotify/track/presentation/controller/TrackController.java backend/track-service/src/main/java/com/spotify/track/infrastructure/security/ backend/track-service/src/test/java/com/spotify/track/application/usecase/ListTracksUseCaseImplTest.java
git commit -m "feat(track): add GET /tracks list-all for search bootstrap"
```

---

### Task 5: docker-compose — Elasticsearch 8.x + free port 8086

**Files:**
- Modify: `backend/docker-compose.yml`

**Interfaces:**
- Consumes: nothing outside the file.
- Produces: `elasticsearch` service bound to host `9200`; **kafka-ui moved 8086 → 8087** so the search-service can own 8086 (spec §3).

- [ ] **Step 1: Change kafka-ui host port to free 8086**

In `backend/docker-compose.yml`, replace the kafka-ui ports block (line 89-91):

```yaml
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "8087:8080"
```

- [ ] **Step 2: Add the elasticsearch service**

Insert after the `track-db` block (before `redis`):

```yaml
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.15.3
    container_name: spotify_elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
      - ingest.geoip.downloader.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data
    restart: always
```

- [ ] **Step 3: Register the `es_data` volume**

In the `volumes:` block (line 98-103), add:

```yaml
  es_data:
```

- [ ] **Step 4: Verify compose file parses**

Run: `docker compose -f backend/docker-compose.yml config --quiet`
Expected: exit 0, no output (config valid).

- [ ] **Step 5: Commit**

```bash
git add backend/docker-compose.yml
git commit -m "chore(infra): add elasticsearch 8.x to compose, move kafka-ui to 8087"
```

---

### Task 6: search-service — module scaffold (pom, parent module, main, security, exception handler)

**Files:**
- Modify: `backend/pom.xml` (register module)
- Create: `backend/search-service/pom.xml`
- Create: `backend/search-service/src/main/resources/application.yml`
- Create: `backend/search-service/src/main/java/com/spotify/search/SearchServiceApplication.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/infrastructure/exception/GlobalExceptionHandler.java`
- Modify: `backend/common-lib/src/main/java/com/spotify/common/infrastructure/security/ServiceSecurityConfig.java`

**Interfaces:**
- Consumes: common-lib (`ApiResponse`, `ServiceSecurityConfig`, `GlobalResponseWrapper`, `GatewayHeaderFilter`), spring-kafka, spring-data-elasticsearch autoconfig.
- Produces: Spring Boot app on port 8086; `ServiceSecurityConfig` gains `/api/v1/search/**` in its security matcher; `GlobalExceptionHandler` maps 400/500 to `ApiResponse.error`. Later tasks add: domain (T7), usecases (T8-10), ES adapter (T11), Kafka consumer (T12), bootstrap (T13), controller (T14).

- [ ] **Step 1: Register the module in the parent pom**

In `backend/pom.xml`, inside `<modules>` (after `track-service`):

```xml
        <module>search-service</module>
```

- [ ] **Step 2: Create `backend/search-service/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.spotify</groupId>
        <artifactId>backend</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>search-service</artifactId>
    <name>spotify-clone-search-service</name>
    <description>Search service - Elasticsearch full-text search over the track catalog</description>

    <dependencies>
        <!-- Internal -->
        <dependency>
            <groupId>com.spotify</groupId>
            <artifactId>common-lib</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>

        <!-- Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- API docs -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>com.spotify.search.SearchServiceApplication</mainClass>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

- [ ] **Step 3: Create `application.yml`**

```yaml
server:
  port: 8086

spring:
  application:
    name: spotify-clone-search-service
  elasticsearch:
    uris: ${ELASTICSEARCH_URIS:http://localhost:9200}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: search-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.spotify.common.event
    enabled: ${KAFKA_ENABLED:true}
  jackson:
    default-property-inclusion: NON_NULL

# Track-service endpoint for the startup bootstrap (spec §6.1)
search:
  track-service:
    base-url: ${TRACK_SERVICE_URL:http://localhost:8085}

# ===== OpenAPI / Swagger UI =====
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
```

- [ ] **Step 4: Create the application main class**

```java
package com.spotify.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication(scanBasePackages = "com.spotify")
public class SearchServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
```

- [ ] **Step 5: Create the search-service exception handler**

```java
package com.spotify.search.infrastructure.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.spotify.common.infrastructure.web.ApiResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/** Maps exceptions to standardized {@link ApiResponse} error bodies (conventions.md §2). */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed: " + ex.getMessage()));
    }

    // Use case guards (blank q, invalid limit) are client errors
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.internalServerError().body(ApiResponse.error("An unexpected error occurred"));
    }
}
```

- [ ] **Step 6: Add `/api/v1/search/**` to the shared security matcher**

In `ServiceSecurityConfig.java` (common-lib), replace the `securityMatcher` line:

```java
                .securityMatcher("/api/v1/playlists/**", "/api/v1/tracks/**", "/api/v1/songs/**", "/api/v1/users/**", "/api/v1/search/**")
```

- [ ] **Step 7: Verify the module compiles (all common-lib tests too)**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -am`
Expected: BUILD SUCCESS (search-service compiles with 0 tests; common-lib 7 pass).

- [ ] **Step 8: Commit**

```bash
git add backend/pom.xml backend/search-service/ backend/common-lib/src/main/java/com/spotify/common/infrastructure/security/ServiceSecurityConfig.java
git commit -m "feat(search): scaffold search-service module (8086, security, exception handler)"
```

---

### Task 7: search-service — domain entity, repository port, commands & DTOs

**Files:**
- Create: `backend/search-service/src/main/java/com/spotify/search/domain/entity/TrackSearchDocument.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/domain/repository/TrackSearchRepository.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/application/dto/IndexTrackCommand.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/application/dto/RemoveTrackCommand.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/application/dto/SearchTracksCommand.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/application/dto/SearchTrackItem.java`

**Interfaces:**
- Produces (consumed by T8-14): `TrackSearchDocument(UUID id, String title, String artist, String album, Long durationMs, String artworkUrl, String audioUrl)` (pure record, Jackson-deserializable for ES `_source`); `TrackSearchRepository` port with `index(TrackSearchDocument)`, `remove(UUID trackId)`, `search(String query, int limit) → List<TrackSearchDocument>`; commands `IndexTrackCommand(TrackSearchDocument document)`, `RemoveTrackCommand(UUID trackId)`, `SearchTracksCommand(String query, int limit)`; DTO `SearchTrackItem` record + `static SearchTrackItem from(TrackSearchDocument)`.

- [ ] **Step 1: Create the domain files**

```java
package com.spotify.search.domain.entity;

import java.util.UUID;

/** Searchable snapshot of a track — mirrors the ES index `tracks` mapping (spec §5). */
public record TrackSearchDocument(
        UUID id,
        String title,
        String artist,
        String album,
        Long durationMs,
        String artworkUrl,
        String audioUrl
) {}
```

```java
package com.spotify.search.domain.repository;

import java.util.List;
import java.util.UUID;

import com.spotify.search.domain.entity.TrackSearchDocument;

/** Port into the Elasticsearch index (spec §3). */
public interface TrackSearchRepository {
    void index(TrackSearchDocument document);

    void remove(UUID trackId);

    List<TrackSearchDocument> search(String query, int limit);
}
```

```java
package com.spotify.search.application.dto;

import com.spotify.search.domain.entity.TrackSearchDocument;

/** Input for indexing one track (from a Kafka event or the bootstrap). */
public record IndexTrackCommand(TrackSearchDocument document) {}
```

```java
package com.spotify.search.application.dto;

import java.util.UUID;

/** Input for removing one track from the index. */
public record RemoveTrackCommand(UUID trackId) {}
```

```java
package com.spotify.search.application.dto;

/** Input for a full-text query — blank q is rejected by the use case. */
public record SearchTracksCommand(String query, int limit) {}
```

```java
package com.spotify.search.application.dto;

import java.util.UUID;

import com.spotify.search.domain.entity.TrackSearchDocument;

/** Search result item returned to the gateway/FE (spec §4). */
public record SearchTrackItem(
        UUID id,
        String title,
        String artist,
        String album,
        String artworkUrl,
        String audioUrl,
        Long durationMs
) {
    public static SearchTrackItem from(TrackSearchDocument doc) {
        return new SearchTrackItem(doc.id(), doc.title(), doc.artist(), doc.album(),
                doc.artworkUrl(), doc.audioUrl(), doc.durationMs());
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q compile -pl search-service`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/search-service/src/main/java/com/spotify/search/domain/ backend/search-service/src/main/java/com/spotify/search/application/
git commit -m "feat(search): add domain entity, repository port, commands and DTOs"
```

---

### Task 8: search-service — `IndexTrackUseCase`

**Files:**
- Create: `backend/search-service/src/main/java/com/spotify/search/application/usecase/IndexTrackUseCase.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/application/usecase/IndexTrackUseCaseImpl.java`
- Test: `backend/search-service/src/test/java/com/spotify/search/application/usecase/IndexTrackUseCaseImplTest.java`

**Interfaces:**
- Consumes: `TrackSearchRepository` port, `IndexTrackCommand` (T7).
- Produces: `IndexTrackUseCase.execute(IndexTrackCommand)` — validates then delegates to the repository.

- [ ] **Step 1: Write the failing test**

```java
package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.IndexTrackCommand;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

        verify(repository).index(doc);
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -Dtest=IndexTrackUseCaseImplTest`
Expected: FAIL — `IndexTrackUseCaseImpl` not found.

- [ ] **Step 3: Write the minimal implementation**

```java
package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.IndexTrackCommand;

/** Command handler: index one track document into Elasticsearch. */
public interface IndexTrackUseCase {
    void execute(IndexTrackCommand command);
}
```

```java
package com.spotify.search.application.usecase;

import org.springframework.stereotype.Service;

import com.spotify.search.application.dto.IndexTrackCommand;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IndexTrackUseCaseImpl implements IndexTrackUseCase {

    private final TrackSearchRepository trackSearchRepository;

    @Override
    public void execute(IndexTrackCommand command) {
        TrackSearchDocument document = command.document();
        if (document == null || document.id() == null) {
            throw new IllegalArgumentException("track document with id is required");
        }
        trackSearchRepository.index(document);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -Dtest=IndexTrackUseCaseImplTest`
Expected: PASS (4).

- [ ] **Step 5: Commit**

```bash
git add backend/search-service/src/main/java/com/spotify/search/application/usecase/ backend/search-service/src/test/java/com/spotify/search/application/usecase/IndexTrackUseCaseImplTest.java
git commit -m "feat(search): add IndexTrackUseCase"
```

---

### Task 9: search-service — `RemoveTrackUseCase`

**Files:**
- Create: `backend/search-service/src/main/java/com/spotify/search/application/usecase/RemoveTrackUseCase.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/application/usecase/RemoveTrackUseCaseImpl.java`
- Test: `backend/search-service/src/test/java/com/spotify/search/application/usecase/RemoveTrackUseCaseImplTest.java`

**Interfaces:**
- Consumes: `TrackSearchRepository`, `RemoveTrackCommand` (T7).
- Produces: `RemoveTrackUseCase.execute(RemoveTrackCommand)`.

- [ ] **Step 1: Write the failing test**

```java
package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.RemoveTrackCommand;
import com.spotify.search.domain.repository.TrackSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -Dtest=RemoveTrackUseCaseImplTest`
Expected: FAIL — class not found.

- [ ] **Step 3: Write the minimal implementation**

```java
package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.RemoveTrackCommand;

/** Command handler: remove one track from the search index. */
public interface RemoveTrackUseCase {
    void execute(RemoveTrackCommand command);
}
```

```java
package com.spotify.search.application.usecase;

import org.springframework.stereotype.Service;

import com.spotify.search.application.dto.RemoveTrackCommand;
import com.spotify.search.domain.repository.TrackSearchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoveTrackUseCaseImpl implements RemoveTrackUseCase {

    private final TrackSearchRepository trackSearchRepository;

    @Override
    public void execute(RemoveTrackCommand command) {
        if (command.trackId() == null) {
            throw new IllegalArgumentException("trackId is required");
        }
        trackSearchRepository.remove(command.trackId());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -Dtest=RemoveTrackUseCaseImplTest`
Expected: PASS (2).

- [ ] **Step 5: Commit**

```bash
git add backend/search-service/src/main/java/com/spotify/search/application/usecase/RemoveTrackUseCase*.java backend/search-service/src/test/java/com/spotify/search/application/usecase/RemoveTrackUseCaseImplTest.java
git commit -m "feat(search): add RemoveTrackUseCase"
```

---

### Task 10: search-service — `SearchTracksUseCase`

**Files:**
- Create: `backend/search-service/src/main/java/com/spotify/search/application/usecase/SearchTracksUseCase.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/application/usecase/SearchTracksUseCaseImpl.java`
- Test: `backend/search-service/src/test/java/com/spotify/search/application/usecase/SearchTracksUseCaseImplTest.java`

**Interfaces:**
- Consumes: `TrackSearchRepository`, `SearchTracksCommand`, `SearchTrackItem` (T7).
- Produces: `SearchTracksUseCase.execute(SearchTracksCommand) → List<SearchTrackItem>`; rejects blank `q`, clamps limit to [1, 50].

- [ ] **Step 1: Write the failing test**

```java
package com.spotify.search.application.usecase;

import com.spotify.search.application.dto.SearchTracksCommand;
import com.spotify.search.application.dto.SearchTrackItem;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -Dtest=SearchTracksUseCaseImplTest`
Expected: FAIL — class not found.

- [ ] **Step 3: Write the minimal implementation**

```java
package com.spotify.search.application.usecase;

import java.util.List;

import com.spotify.search.application.dto.SearchTracksCommand;
import com.spotify.search.application.dto.SearchTrackItem;

/** Command handler: full-text search over indexed tracks. */
public interface SearchTracksUseCase {
    List<SearchTrackItem> execute(SearchTracksCommand command);
}
```

```java
package com.spotify.search.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spotify.search.application.dto.SearchTracksCommand;
import com.spotify.search.application.dto.SearchTrackItem;
import com.spotify.search.domain.repository.TrackSearchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchTracksUseCaseImpl implements SearchTracksUseCase {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final TrackSearchRepository trackSearchRepository;

    @Override
    public List<SearchTrackItem> execute(SearchTracksCommand command) {
        if (command.query() == null || command.query().isBlank()) {
            throw new IllegalArgumentException("q is required");
        }
        int limit = Math.max(DEFAULT_LIMIT, Math.min(command.limit(), MAX_LIMIT));
        // The Elasticsearch adapter owns relevance ranking (multi-match title^3/artist^2/album).
        return trackSearchRepository.search(command.query().trim(), limit).stream()
                .map(SearchTrackItem::from)
                .toList();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -Dtest=SearchTracksUseCaseImplTest`
Expected: PASS (4).

- [ ] **Step 5: Commit**

```bash
git add backend/search-service/src/main/java/com/spotify/search/application/usecase/SearchTracksUseCase*.java backend/search-service/src/test/java/com/spotify/search/application/usecase/SearchTracksUseCaseImplTest.java
git commit -m "feat(search): add SearchTracksUseCase"
```

---

### Task 11: search-service — Elasticsearch adapter (index/remove/search + index setup)

**Files:**
- Create: `backend/search-service/src/main/java/com/spotify/search/infrastructure/config/ElasticsearchConfig.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/infrastructure/search/TrackElasticsearchRepository.java`

**Interfaces:**
- Consumes: `TrackSearchRepository` port (T7), `TrackSearchDocument` (T7), auto-configured `co.elastic.clients.elasticsearch.ElasticsearchClient` (from `spring-boot-starter-data-elasticsearch` + `spring.elasticsearch.uris`).
- Produces: `TrackElasticsearchRepository implements TrackSearchRepository` — index `tracks` (mapping per spec §5), ES multi-match `title^3, artist^2, album` with `fuzziness AUTO`, size=limit. Not unit-tested (needs ES — verified at smoke + integration, spec §8).

- [ ] **Step 1: Create `ElasticsearchConfig` (index constants + mapping)**

```java
package com.spotify.search.infrastructure.config;

/**
 * Index name + mapping for the search index. The mapping mirrors the spec §5 contract
 * (artwork/audio URLs stored but not analyzed; duration is a numeric filterable field).
 */
public final class ElasticsearchConfig {
    public static final String TRACK_INDEX = "tracks";

    public static final String TRACK_INDEX_MAPPING = """
            {
              "mappings": {
                "properties": {
                  "id": {"type": "keyword"},
                  "title": {"type": "text"},
                  "artist": {"type": "text"},
                  "album": {"type": "text"},
                  "artworkUrl": {"type": "keyword", "index": false},
                  "audioUrl": {"type": "keyword", "index": false},
                  "durationMs": {"type": "long"}
                }
              }
            }
            """;

    private ElasticsearchConfig() {
        // constants holder
    }
}
```

- [ ] **Step 2: Create the repository adapter**

```java
package com.spotify.search.infrastructure.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;
import com.spotify.search.infrastructure.config.ElasticsearchConfig;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Elasticsearch adapter over the `tracks` index (spec §5). */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackElasticsearchRepository implements TrackSearchRepository {

    private final ElasticsearchClient client;

    /** Creates the index with the spec mapping when missing — safe to call on every startup. */
    public void ensureIndex() {
        try {
            boolean exists = client.indices().exists(e -> e.index(ElasticsearchConfig.TRACK_INDEX)).value();
            if (!exists) {
                client.indices().create(c -> c.index(ElasticsearchConfig.TRACK_INDEX)
                        .withJson(new StringReader(ElasticsearchConfig.TRACK_INDEX_MAPPING)));
                log.info("Created Elasticsearch index '{}'", ElasticsearchConfig.TRACK_INDEX);
            }
        } catch (IOException | ElasticsearchException e) {
            log.error("Could not ensure index '{}' — search may be unavailable", ElasticsearchConfig.TRACK_INDEX, e);
        }
    }

    @Override
    public void index(TrackSearchDocument document) {
        try {
            client.index(i -> i.index(ElasticsearchConfig.TRACK_INDEX)
                    .id(document.id().toString())
                    .document(document));
        } catch (IOException e) {
            throw new RuntimeException("Failed to index track " + document.id(), e);
        }
    }

    @Override
    public void remove(UUID trackId) {
        try {
            client.delete(d -> d.index(ElasticsearchConfig.TRACK_INDEX).id(trackId.toString()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to remove track " + trackId + " from search index", e);
        }
    }

    @Override
    public List<TrackSearchDocument> search(String query, int limit) {
        try {
            SearchResponse<TrackSearchDocument> response = client.search(
                    s -> s.index(ElasticsearchConfig.TRACK_INDEX)
                            .size(limit)
                            .query(q -> q.multiMatch(m -> m
                                    .query(query)
                                    .fields(List.of("title^3", "artist^2", "album"))
                                    .fuzziness("AUTO"))),
                    TrackSearchDocument.class);
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Search failed", e);
        }
    }
}
```

- [ ] **Step 3: Verify compile**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q compile -pl search-service`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/search-service/src/main/java/com/spotify/search/infrastructure/
git commit -m "feat(search): add Elasticsearch adapter for tracks index"
```

---

### Task 12: search-service — Kafka consumer for track events

**Files:**
- Create: `backend/search-service/src/main/java/com/spotify/search/infrastructure/kafka/TrackEventConsumer.java`
- Test: `backend/search-service/src/test/java/com/spotify/search/infrastructure/kafka/TrackEventConsumerTest.java`

**Interfaces:**
- Consumes: `TrackEventEnvelope` (common-lib, T1), `IndexTrackUseCase` (T8), `RemoveTrackUseCase` (T9), `TrackSearchDocument` (T7).
- Produces: `TrackEventConsumer` with `@KafkaListener(topics = "spotify.track.events", groupId = "search-service-group")`; routes `TRACK_UPLOADED`/`TRACK_UPDATED` → `IndexTrackCommand`, `TRACK_REMOVED` → `RemoveTrackCommand`, `TRACK_AUDIO_UPLOADED` → no-op (spec §3). Gated with `@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)`.

- [ ] **Step 1: Write the failing test**

```java
package com.spotify.search.infrastructure.kafka;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.common.event.TrackEventType;
import com.spotify.search.application.dto.IndexTrackCommand;
import com.spotify.search.application.dto.RemoveTrackCommand;
import com.spotify.search.application.usecase.IndexTrackUseCase;
import com.spotify.search.application.usecase.RemoveTrackUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrackEventConsumerTest {

    @Mock
    private IndexTrackUseCase indexTrackUseCase;
    @Mock
    private RemoveTrackUseCase removeTrackUseCase;

    @InjectMocks
    private TrackEventConsumer consumer;

    private TrackEventEnvelope envelope(TrackEventType type, String id) {
        return new TrackEventEnvelope(type, "evt-1", "2026-08-29T10:15:30",
                new TrackEventEnvelope.TrackPayload(id, "Free Spirit", "Khalid",
                        "Free Spirit (Explicit)", 182_000L, "https://artwork.png", null));
    }

    @Test
    void should_Index_when_Uploaded() {
        consumer.onTrackEvent(envelope(TrackEventType.TRACK_UPLOADED, "9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b"));

        verify(indexTrackUseCase).execute(any(IndexTrackCommand.class));
        verify(removeTrackUseCase, never()).execute(any());
    }

    @Test
    void should_Index_when_Updated() {
        consumer.onTrackEvent(envelope(TrackEventType.TRACK_UPDATED, "9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b"));

        verify(indexTrackUseCase).execute(any(IndexTrackCommand.class));
        verify(removeTrackUseCase, never()).execute(any());
    }

    @Test
    void should_Remove_when_Removed() {
        consumer.onTrackEvent(envelope(TrackEventType.TRACK_REMOVED, "9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b"));

        verify(removeTrackUseCase).execute(any(RemoveTrackCommand.class));
        verify(indexTrackUseCase, never()).execute(any());
    }

    @Test
    void should_Ignore_when_AudioUploaded() {
        consumer.onTrackEvent(envelope(TrackEventType.TRACK_AUDIO_UPLOADED, "9a0a7e1a-8a10-43b0-a1c7-0b0e4f1d2a3b"));

        verify(indexTrackUseCase, never()).execute(any());
        verify(removeTrackUseCase, never()).execute(any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -Dtest=TrackEventConsumerTest`
Expected: FAIL — class not found.

- [ ] **Step 3: Write the minimal implementation**

```java
package com.spotify.search.infrastructure.kafka;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.search.application.dto.IndexTrackCommand;
import com.spotify.search.application.dto.RemoveTrackCommand;
import com.spotify.search.application.usecase.IndexTrackUseCase;
import com.spotify.search.application.usecase.RemoveTrackUseCase;
import com.spotify.search.domain.entity.TrackSearchDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes track events from the shared topic and keeps the ES index in sync.
 * TRACK_AUDIO_UPLOADED does not affect the index (spec §3) — logged only.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TrackEventConsumer {

    private static final String TRACK_EVENTS_TOPIC = "spotify.track.events";

    private final IndexTrackUseCase indexTrackUseCase;
    private final RemoveTrackUseCase removeTrackUseCase;

    @KafkaListener(topics = TRACK_EVENTS_TOPIC, groupId = "search-service-group")
    public void onTrackEvent(TrackEventEnvelope envelope) {
        log.debug("Received track event: {} | eventId={}", envelope.eventType(), envelope.eventId());
        switch (envelope.eventType()) {
            case TRACK_UPLOADED, TRACK_UPDATED -> indexTrackUseCase.execute(new IndexTrackCommand(toDocument(envelope.track())));
            case TRACK_REMOVED -> removeTrackUseCase.execute(new RemoveTrackCommand(UUID.fromString(envelope.track().id())));
            case TRACK_AUDIO_UPLOADED -> log.debug("Audio upload event — index unaffected");
        }
    }

    private TrackSearchDocument toDocument(TrackEventEnvelope.TrackPayload track) {
        return new TrackSearchDocument(
                UUID.fromString(track.id()),
                track.title(),
                track.artist(),
                track.album(),
                track.durationMs(),
                track.artworkUrl(),
                track.audioUrl());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -Dtest=TrackEventConsumerTest`
Expected: PASS (4).

- [ ] **Step 5: Commit**

```bash
git add backend/search-service/src/main/java/com/spotify/search/infrastructure/kafka/ backend/search-service/src/test/java/com/spotify/search/infrastructure/kafka/
git commit -m "feat(search): consume track events to index/remove in Elasticsearch"
```

---

### Task 13: search-service — startup bootstrap (reindex history from track-service)

**Files:**
- Create: `backend/search-service/src/main/java/com/spotify/search/infrastructure/bootstrap/TrackIndexBootstrap.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/infrastructure/bootstrap/TrackBootstrapFetcher.java`
- Create: `backend/search-service/src/main/java/com/spotify/search/infrastructure/bootstrap/RestTrackBootstrapFetcher.java`
- Test: `backend/search-service/src/test/java/com/spotify/search/infrastructure/bootstrap/TrackIndexBootstrapTest.java`

**Interfaces:**
- Consumes: `TrackSearchRepository` (T7), `TrackEventEnvelope.TrackPayload` (common-lib), `ApiResponse` (common-lib), `@Value("${search.track-service.base-url}")` (T6 yml).
- Produces: `TrackIndexBootstrap implements ApplicationRunner` — calls `GET {track-service}/api/v1/tracks`, indexes each payload; swallows failures (never crash app). `TrackBootstrapFetcher` port + `RestTrackBootstrapFetcher` adapter using `RestClient`. Gated with `@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)`.

- [ ] **Step 1: Write the failing test**

```java
package com.spotify.search.infrastructure.bootstrap;

import com.spotify.common.event.TrackEventEnvelope;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrackIndexBootstrapTest {

    @Mock
    private TrackBootstrapFetcher fetcher;
    @Mock
    private TrackSearchRepository repository;

    private TrackIndexBootstrap bootstrap;

    @Test
    void should_IndexAllTracks_when_FetcherReturnsData() {
        TrackEventEnvelope.TrackPayload a = new TrackEventEnvelope.TrackPayload("1",
                "Free Spirit", "Khalid", "Free Spirit (Explicit)", 182_000L, "https://a.png", null);
        TrackEventEnvelope.TrackPayload b = new TrackEventEnvelope.TrackPayload("2",
                "Ocean Front Apt.", "ayokay", "Digital Dreamscape", 132_000L, "https://b.png", null);
        when(fetcher.fetchAll()).thenReturn(List.of(a, b));
        bootstrap = new TrackIndexBootstrap(fetcher, repository);

        bootstrap.run(any(ApplicationArguments.class));

        verify(repository, times(2)).index(any(TrackSearchDocument.class));
    }

    @Test
    void should_NotCrash_when_TrackServiceUnreachable() {
        doThrow(new RuntimeException("connection refused")).when(fetcher).fetchAll();
        bootstrap = new TrackIndexBootstrap(fetcher, repository);

        assertDoesNotThrow(() -> bootstrap.run(any(ApplicationArguments.class)));

        verify(repository, times(0)).index(any(TrackSearchDocument.class));
    }
}
```

> Note: `assertDoesNotThrow` + `verify(times(0))` replace the strict-stubbing concern without needing `lenient()`; use `assertDoesNotThrow` if the second test needs it. Add import `org.mockito.Mockito.when` and `static org.mockito.Mockito.verify`.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -Dtest=TrackIndexBootstrapTest`
Expected: FAIL — `when`/`TrackIndexBootstrap` not found (add the `when` import as noted above).

- [ ] **Step 3: Write the bootstrap**

```java
package com.spotify.search.infrastructure.bootstrap;

import java.util.List;

import com.spotify.common.event.TrackEventEnvelope.TrackPayload;

/** Pulls the historical catalog once at startup (spec §6.1). */
public interface TrackBootstrapFetcher {
    List<TrackPayload> fetchAll();
}
```

```java
package com.spotify.search.infrastructure.bootstrap;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.spotify.common.event.TrackEventEnvelope.TrackPayload;
import com.spotify.common.infrastructure.web.ApiResponse;

/** HTTP adapter over {@code GET {track-service}/api/v1/tracks} (list-all serializes the catalog). */
@Component
public class RestTrackBootstrapFetcher implements TrackBootstrapFetcher {

    private static final ParameterizedTypeReference<ApiResponse<List<TrackPayload>>> RESPONSE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public RestTrackBootstrapFetcher(
            @Value("${search.track-service.base-url:http://localhost:8085}") String trackServiceBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(trackServiceBaseUrl).build();
    }

    @Override
    public List<TrackPayload> fetchAll() {
        ApiResponse<List<TrackPayload>> response = restClient.get()
                .uri("/api/v1/tracks")
                .retrieve()
                .body(RESPONSE);
        return response != null && response.data() != null ? response.data() : List.of();
    }
}
```

```java
package com.spotify.search.infrastructure.bootstrap;

import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.spotify.common.event.TrackEventEnvelope.TrackPayload;
import com.spotify.search.domain.entity.TrackSearchDocument;
import com.spotify.search.domain.repository.TrackSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reindexes existing tracks on startup so the index is not empty when Elasticsearch comes
 * up after the catalog was seeded. Elasticsearch unavailability must NOT crash the app —
 * runtime Kafka events backfill, and the bootstrap logs the failure instead (spec §6.4).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TrackIndexBootstrap implements ApplicationRunner {

    private final TrackBootstrapFetcher fetcher;
    private final TrackSearchRepository trackSearchRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            var tracks = fetcher.fetchAll();
            log.info("Bootstrap: indexing {} tracks from track-service", tracks.size());
            for (TrackPayload payload : tracks) {
                trackSearchRepository.index(toDocument(payload));
            }
        } catch (Exception e) {
            log.error("Bootstrap reindex skipped (indexing will catch up via Kafka events): {}", e.getMessage());
        }
    }

    private TrackSearchDocument toDocument(TrackPayload payload) {
        return new TrackSearchDocument(
                UUID.fromString(payload.id()),
                payload.title(),
                payload.artist(),
                payload.album(),
                payload.durationMs(),
                payload.artworkUrl(),
                payload.audioUrl());
    }
}
```

- [ ] **Step 4: Fix the test to be complete/accurate**

Replace `should_NotCrash_when_TrackServiceUnreachable`'s imports with the strict-stub-safe version:

```java
    ...import static org.mockito.Mockito.verify;
    import static org.mockito.Mockito.never;
    ...
    verify(repository, never()).index(any(TrackSearchDocument.class));
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service -Dtest=TrackIndexBootstrapTest`
Expected: PASS (2).

- [ ] **Step 6: Commit**

```bash
git add backend/search-service/src/main/java/com/spotify/search/infrastructure/bootstrap/ backend/search-service/src/test/java/com/spotify/search/infrastructure/bootstrap/
git commit -m "feat(search): bootstrap reindex from track-service on startup"
```

---

### Task 14: search-service — `SearchController` (public API)

**Files:**
- Create: `backend/search-service/src/main/java/com/spotify/search/presentation/controller/SearchController.java`

**Interfaces:**
- Consumes: `SearchTracksUseCase` (T10), `SearchTracksCommand`/`SearchTrackItem` (T7).
- Produces: `GET /api/v1/search/tracks?q=&limit=` (default limit 10, @Validated `@NotBlank q`, `@Min(1) @Max(50) limit`). Response is wrapped by common-lib `GlobalResponseWrapper` into `ApiResponse<List<SearchTrackItem>>`. No unit test — matches existing track/playlist convention (usecases carry the logic; behavior verified in smoke).

- [ ] **Step 1: Create the controller**

```java
package com.spotify.search.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.search.application.dto.SearchTracksCommand;
import com.spotify.search.application.dto.SearchTrackItem;
import com.spotify.search.application.usecase.SearchTracksUseCase;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/** Search API — {@code GET /api/v1/search/tracks?q=&limit=} (spec §4). */
@Validated
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchTracksUseCase searchTracksUseCase;

    @GetMapping("/tracks")
    public ResponseEntity<List<SearchTrackItem>> searchTracks(
            @RequestParam("q") @NotBlank String q,
            @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(searchTracksUseCase.execute(new SearchTracksCommand(q, limit)));
    }
}
```

- [ ] **Step 2: Verify compile + full search-service tests green**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test -pl search-service`
Expected: BUILD SUCCESS (14 tests: 4 Index + 2 Remove + 4 Search + 4 Consumer).

- [ ] **Step 3: Commit**

```bash
git add backend/search-service/src/main/java/com/spotify/search/presentation/
git commit -m "feat(search): add GET /api/v1/search/tracks endpoint"
```

---

### Task 15: gateway — route `/api/v1/search/**` → 8086

**Files:**
- Modify: `gateway/src/main/java/com/spotify/gateway/config/GatewayConfig.java`

**Interfaces:**
- Consumes: `JwtAuthFilter` bean (existing).
- Produces: `search-service` route: `/api/v1/search/**` + JWT filter → `http://localhost:8086`.

- [ ] **Step 1: Add the route**

In `GatewayConfig.java`, after the `track-service` route (line 27-29):

```java
                .route("search-service", r -> r.path("/api/v1/search/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("http://localhost:8086"))
```

- [ ] **Step 2: Verify it compiles**

Run: `cd gateway && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test`
Expected: BUILD SUCCESS (gateway has no tests; compiles clean). If `gateway/pom.xml` has no parent, use the repo's maven dist with `cd gateway && mvn -q compile`.

- [ ] **Step 3: Commit**

```bash
git add gateway/src/main/java/com/spotify/gateway/config/GatewayConfig.java
git commit -m "feat(gateway): route /api/v1/search/** to search-service"
```

---

### Task 16: FE — real search API service (`frontend/services/api/searchService.ts`)

**Files:**
- Create: `frontend/services/api/searchService.ts`
- Test: `frontend/services/api/__tests__/searchService.test.ts`

**Interfaces:**
- Consumes: `api` + `ApiResponse` + `unwrap` from `@/lib/api-client`.
- Produces: `SearchApiService.search(query, limit?) → Promise<SearchItem[]>` calling `GET /api/v1/search/tracks` (params `q`, `limit`) and unwrapping the envelope; type `SearchItem { id, title, artist, album, artworkUrl?, audioUrl?, durationMs? }`. Keeps the existing pure `ServicesSearchService` (`services/search/searchService.ts`) untouched — spec §7.

- [ ] **Step 1: Write the failing test**

```ts
import { describe, it, expect, vi, beforeEach } from "vitest";

// Mock the api-client module entirely — no axios network in unit tests.
vi.mock("@/lib/api-client", () => ({
  api: { get: vi.fn() },
  unwrap: (envelope: { data: unknown }) => envelope.data,
  type ApiResponse: true,
}));

import { api } from "@/lib/api-client";
import { SearchApiService } from "@/services/api/searchService";

describe("SearchApiService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("returns [] without calling the API when query is blank", async () => {
    expect(await SearchApiService.search("   ")).toEqual([]);
    expect(api.get).not.toHaveBeenCalled();
  });

  it("calls /search/tracks with q and limit params, then unwraps data", async () => {
    const envelope = {
      success: true,
      data: [{ id: "1", title: "Free Spirit", artist: "Khalid", album: "Free Spirit (Explicit)" }],
      message: "ok",
      timestamp: "2026-08-29T10:00:00Z",
    };
    vi.mocked(api.get).mockResolvedValue(envelope);

    const result = await SearchApiService.search("khalid", 5);

    expect(api.get).toHaveBeenCalledWith("/search/tracks", { params: { q: "khalid", limit: 5 } });
    expect(result).toEqual(envelope.data);
  });

  it("passes the default limit 10 when none given", async () => {
    vi.mocked(api.get).mockResolvedValue({
      success: true,
      data: [],
      message: "ok",
      timestamp: "2026-08-29T10:00:00Z",
    });

    await SearchApiService.search("ocean");

    expect(api.get).toHaveBeenCalledWith("/search/tracks", { params: { q: "ocean", limit: 10 } });
  });
});
```

> Note: `type ApiResponse: true` is invalid in an object literal — instead alias the mock's type. See Step 3 for the corrected import-free version.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npm run test -- src/services/api/__tests__/searchService.test.ts`
Expected: FAIL — module not found.

- [ ] **Step 3: Write the implementation + corrected test**

Create `frontend/services/api/searchService.ts`:

```ts
/**
 * Search API (search-service) — real full-text search behind the gateway.
 * FE SearchBar switches from the TRACK_INDEX mock to this (spec §7).
 */
import { api, type ApiResponse, unwrap } from "@/lib/api-client";

/** Search result item as returned by the backend (spec §4 fields). */
export interface SearchItem {
  id: string;
  title: string;
  artist: string;
  album: string;
  artworkUrl?: string;
  audioUrl?: string;
  durationMs?: number;
}

export class SearchApiService {
  /** GET /api/v1/search/tracks?q=&limit= → unwrapped SearchItem[]. */
  static async search(query: string, limit = 10): Promise<SearchItem[]> {
    const q = query.trim();
    if (!q) return [];
    const envelope = await api.get<ApiResponse<SearchItem[]>>("/search/tracks", {
      params: { q, limit },
    });
    return unwrap(envelope);
  }
}
```

Correct the test's mock (`vi.mock` cannot carry `type` keys — drop it; the call sites infer from the mocked `api.get`):

```ts
vi.mock("@/lib/api-client", () => ({
  api: { get: vi.fn() },
  unwrap: (envelope: { data: unknown }) => envelope.data,
}));
```

- [ ] **Step 4: Run tests to verify green**

Run: `cd frontend && npm run test -- src/services/api/__tests__/searchService.test.ts`
Expected: PASS (3).

- [ ] **Step 5: Commit**

```bash
git add frontend/services/api/searchService.ts frontend/services/api/__tests__/searchService.test.ts
git commit -m "feat(frontend): add real search API service"
```

---

### Task 17: FE — wire SearchBar to the real API (React Query + debounce)

**Files:**
- Modify: `frontend/lib/queryKeys.ts`
- Modify: `frontend/components/search/SearchBar.tsx`
- Test: `frontend/components/search/__tests__/SearchBar.test.tsx` (update — mock the API module)

**Interfaces:**
- Consumes: `SearchApiService.search` (T16), `SearchService` pure logic (unchanged), `usePlayerStore` (unchanged), `queryKeys`.
- Produces: SearchBar debounces input 300 ms, queries `queryKeys.search.tracks(debounced)`, maps `SearchItem` → the existing `SearchResult`/store `Track` shapes, and no longer imports `TRACK_INDEX`. Suggestions re-derived from live results via the existing pure `SearchService.searchSuggestions`.

- [ ] **Step 1: Add `search` query keys**

In `frontend/lib/queryKeys.ts`, before the closing `} as const;`:

```ts
  search: {
    all: ["search"] as const,
    tracks: (q: string) => [...queryKeys.search.all, "tracks", q] as const,
  },
```

- [ ] **Step 2: Update the SearchBar tests (mock the API module)**

Replace `frontend/components/search/__tests__/SearchBar.test.tsx` — wraps render in a `QueryClientProvider` (React Query throws without it):

```tsx
import { describe, it, expect, beforeEach, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { SearchBar } from "@/components/search/SearchBar";
import { usePlayerStore } from "@/hooks/usePlayerStore";
import { SearchApiService } from "@/services/api/searchService";

vi.mock("@/services/api/searchService", () => ({
  SearchApiService: { search: vi.fn() },
}));

const TRACKS = [
  { id: "t1", title: "Free Spirit", artist: "Khalid", album: "Free Spirit (Explicit)", durationMs: 182000 },
  { id: "t2", title: "Ocean Front Apt.", artist: "ayokay", album: "Digital Dreamscape", durationMs: 132000, artworkUrl: "/figma/chill-mix.png" },
];

function mockApi(query: string) {
  const q = query.trim().toLowerCase();
  const hits = TRACKS.filter(
    (t) =>
      t.title.toLowerCase().includes(q) ||
      t.artist.toLowerCase().includes(q) ||
      t.album.toLowerCase().includes(q)
  );
  vi.mocked(SearchApiService.search).mockImplementation(async () =>
    query.trim() ? hits : []
  );
}

function renderSearchBar() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <SearchBar />
    </QueryClientProvider>
  );
}

function resetPlayer() {
  localStorage.clear();
  usePlayerStore.setState({
    isPlaying: false,
    currentTrack: null,
    volume: 0.7,
    progress: 0,
    queue: [],
    queueIndex: -1,
  });
}

describe("SearchBar", () => {
  beforeEach(() => {
    resetPlayer();
    vi.mocked(SearchApiService.search).mockReset();
  });

  it("renders an accessible combobox with expanded state", () => {
    renderSearchBar();
    const input = screen.getByRole("combobox");
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute("aria-expanded", "false");
  });

  it("queries the API and shows suggestions + song results while typing", async () => {
    mockApi("free");
    renderSearchBar();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "free" } });

    await waitFor(() => {
      expect(SearchApiService.search).toHaveBeenCalledWith("free", 10);
    });
    expect(screen.getByLabelText("Tìm kiếm Free Spirit")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Phát bài hát Free Spirit — Khalid")
    ).toBeInTheDocument();
  });

  it("does not fire a request for a blank query", async () => {
    renderSearchBar();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "   " } });
    await waitFor(() => {
      expect(screen.getByRole("combobox")).toHaveAttribute("aria-expanded", "false");
    });
    expect(SearchApiService.search).not.toHaveBeenCalled();
  });

  it("selecting a song plays it and closes the dropdown", async () => {
    mockApi("ocean");
    renderSearchBar();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "ocean" } });

    await waitFor(() => {
      expect(screen.getByLabelText("Phát bài hát Ocean Front Apt. — ayokay")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByLabelText("Phát bài hát Ocean Front Apt. — ayokay"));

    expect(usePlayerStore.getState().isPlaying).toBe(true);
    expect(usePlayerStore.getState().currentTrack?.title).toBe("Ocean Front Apt.");
    expect(usePlayerStore.getState().currentTrack?.imageUrl).toBe("/figma/chill-mix.png");
    expect(screen.getByRole("combobox")).toHaveAttribute("aria-expanded", "false");
  });

  it("shows no-results state when the API returns nothing", async () => {
    vi.mocked(SearchApiService.search).mockResolvedValue([]);
    renderSearchBar();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "zzzz" } });

    await waitFor(() => {
      expect(screen.getByText(/No results for/i)).toBeInTheDocument();
    });
  });

  it("closes on Escape", async () => {
    mockApi("free");
    renderSearchBar();
    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "free" } });
    await waitFor(() => {
      expect(screen.getByLabelText("Tìm kiếm Free Spirit")).toBeInTheDocument();
    });
    fireEvent.keyDown(input, { key: "Escape" });
    expect(input).toHaveAttribute("aria-expanded", "false");
  });

  it("closes when clicking outside", async () => {
    mockApi("free");
    renderSearchBar();
    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "free" } });
    await waitFor(() => {
      expect(screen.getByLabelText("Tìm kiếm Free Spirit")).toBeInTheDocument();
    });
    fireEvent.mouseDown(document.body);
    expect(input).toHaveAttribute("aria-expanded", "false");
  });
});
```

- [ ] **Step 3: Rewrite `SearchBar.tsx` to fetch from the API with a debounce**

Replace the mock-import and the `results`/`suggestions` derivation. Top of file changes:

```tsx
import { useCallback, useEffect, useId, useMemo, useRef, useState } from "react";
import { Search as SearchIcon, X } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Input } from "@/components/ui/input";
import { SearchResultRow } from "@/components/search/SearchResultRow";
import { SearchSuggestionRow } from "@/components/search/SearchSuggestionRow";
import { usePlayerStore } from "@/hooks/usePlayerStore";
import { queryKeys } from "@/lib/queryKeys";
import { SearchService } from "@/services/search/searchService";
import { SearchApiService, type SearchItem } from "@/services/api/searchService";
```

Replace the state + derived block (current lines 17-31) with:

```tsx
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [open, setOpen] = useState(false);
  const listId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const setCurrentTrack = usePlayerStore((s) => s.setCurrentTrack);
  const setIsPlaying = usePlayerStore((s) => s.setIsPlaying);

  // Debounce 300ms — avoid one request per keystroke while typing.
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(timer);
  }, [query]);

  const tracksQuery = useQuery({
    queryKey: queryKeys.search.tracks(debouncedQuery),
    queryFn: () => SearchApiService.search(debouncedQuery),
    enabled: debouncedQuery.trim() !== "",
  });

  // Backend results are authoritative; map them onto the shapes the pure logic + rows expect.
  const liveIndex = useMemo<SearchItem[]>(() => tracksQuery.data ?? [], [tracksQuery.data]);
  const results = useMemo(
    () =>
      liveIndex.map((t) => ({
        id: t.id,
        title: t.title,
        artist: t.artist,
        album: t.album,
        coverUrl: t.artworkUrl,
      })),
    [liveIndex]
  );
  const suggestions = useMemo(
    () =>
      liveIndex.flatMap((t) => [
        { id: `${t.id}-title`, text: t.title },
        { id: `${t.id}-artist`, text: t.artist },
      ]),
    [liveIndex]
  );
  const isEmpty = query.trim() === "";
  const showDropdown = open && !isEmpty;
  const hasResults = suggestions.length > 0 || results.length > 0;
```

Replace `handleSelectTrack` (current lines 55-70):

```tsx
  // Chọn bài hát → phát trên Player (giống MusicCard) và đóng dropdown.
  const handleSelectTrack = useCallback(
    (result: { id: string; title: string; artist: string; coverUrl?: string }) => {
      const track = liveIndex.find((t) => t.id === result.id);
      setCurrentTrack({
        id: result.id,
        title: result.title,
        artist: result.artist,
        imageUrl: result.coverUrl ?? track?.artworkUrl ?? "",
        duration: track?.durationMs ? Math.round(track.durationMs / 1000) : 0,
        audioUrl: track?.audioUrl,
      });
      setIsPlaying(true);
      setOpen(false);
    },
    [liveIndex, setCurrentTrack, setIsPlaying]
  );
```

Remove the `import { TRACK_INDEX } from "@/lib/musicData";` line — it is no longer referenced.

- [ ] **Step 4: Run the full FE test suite**

Run: `cd frontend && npm run test`
Expected: PASS — existing 26 + searchService test 3 + updated SearchBar suite (count grows by the new API-service tests).

- [ ] **Step 5: Commit**

```bash
git add frontend/lib/queryKeys.ts frontend/components/search/ frontend/services/api/
git commit -m "feat(frontend): SearchBar queries real search API with debounce"
```

---

### Task 18: docs — status + rules inventory (mark search-service done)

**Files:**
- Modify: `PROJECT_STATUS.md`
- Modify: `.claude/rules/context.md`

**Interfaces:**
- Consumes: the delivered feature state from Tasks 1-17.

- [ ] **Step 1: Update `PROJECT_STATUS.md`**

- Giai đoạn: add a line noting search-service built (or promote to a new phase line under "Dự kiến" — keep Phase E as-is, add a "Phase E+ search" note under Frontend inventory + Next actions item 9 = ✅).
- Frontend inventory → Services & tests: add `services/api/searchService.ts` (real `/search/tracks`), note SearchBar no longer uses `TRACK_INDEX`; update Vitest count (26 + new).
- Backend inventory: add `search-service` bullet (port 8086, Clean Arch, ES index `tracks`, Kafka consumer `spotify.track.events`, bootstrap from `GET /tracks`; test count).
- Track-service bullet: note event payload now full track + Kafka publisher + `GET /tracks` list-all.
- Backlog list: remove `search-service` (now done), keep `user-service`.
- Next actions: mark item 9 search-service done, keep remaining.

- [ ] **Step 2: Update `.claude/rules/context.md`**

- Microservices intro: add `search-service` to the physically-repo'd services and gateway route list (`search 8086`).
- Service table: change `search-service` row from 🔴 Backlog to ✅ `backend/search-service/` (port **8086**) — "Elasticsearch full-text search (index tracks, Kafka events + bootstrap)".
- `context.md` line 19: add `search 8086` to gateway routing text and mention ES service in docker-compose.

- [ ] **Step 3: Verify docs build nothing — but run the backend gate**

Run: `cd backend && /d/_mvn_tool/apache-maven-3.9.12/bin/mvn -q test`
Expected: BUILD SUCCESS — common-lib 7, auth 13, playlist 27, track 26, search 14 → 87 xanh.
Then: `cd frontend && npm run test` → all frontend tests green.

- [ ] **Step 4: Commit**

```bash
git add PROJECT_STATUS.md .claude/rules/context.md
git commit -m "docs: mark search-service MVP complete (status + context rules)"
```

---

## Smoke verification (after all tasks — manual, spec §9)

Checklist — run with the full stack up:

- [ ] `cd backend && docker compose up -d` → wait `docker ps` shows `elasticsearch` healthy on 9200, `kafka` up; `curl -s localhost:9200` returns cluster JSON.
- [ ] Start auth (8081), playlist (8084), track (8085), search (8086), gateway (9000) with `KAFKA_ENABLED=true`. Watch track-service logs: `[TRACK_EVENT]` absent until enabled, then bootstrap in search-service logs: `Bootstrap: indexing 6 tracks`.
- [ ] `curl 'http://localhost:9000/api/v1/search/tracks?q=Khalid'` with a JWT → 200 `ApiResponse` containing the seeded Khalid track.
- [ ] Without token → 401 (gateway JWT filter proves `/api/v1/search/**` is protected).
- [ ] Create a track via `POST /api/v1/tracks` (with token) → within ~2 s, `GET /search/tracks?q=<new title>` returns it (event-driven index, spec §6.2/acceptance).
- [ ] FE: login → type in the header SearchBar → results come from the live API (no mock), and picking one plays audio (MinIO URL resolves).
- [ ] Restart search-service: index refreshes from bootstrap (idempotent) — no duplicates.

## Self-review notes (from spec §11)

- **Spec coverage:** §2 events (T1/T3/T12) ✓; §3 track changes — event payload (T2), publisher (T3), list-all (T4); search components (T6-14); docker-compose ES (T5); gateway (T15) ✓; §4 API contract (T14) ✓; §5 mapping (T11) ✓; §6 bootstrap (T13) + runtime flow (T12/T14) ✓; §7 FE (T16/T17) ✓; §8 tests (T1-17) ✓; §10 deliverables (all) ✓.
- **Out-of-scope recorded in spec §1** — playlist search, pagination sâu, DLQ: none implemented (YAGNI).
- **Type consistency:** `TrackEventEnvelope.TrackPayload` field names used identically in T3 (mapper), T12 (consumer), T13 (bootstrap). `TrackSearchDocument` constructor order `(UUID, String, String, String, Long, String, String)` is consistent across T7/T8/T11/T12/T13. `SearchTrackItem.from` mirrors the document accessors.