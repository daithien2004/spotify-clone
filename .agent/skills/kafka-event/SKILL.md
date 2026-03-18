---
name: kafka-event
description: Generate Kafka event flow between microservices
---

# Skill: kafka-event

Chu trình truyền thông điệp giữa các microservices qua Kafka, tuân thủ Clean Architecture.

## 1. Topic & Group Naming

- **Topic:** `{service-name}.{entity}.{event}` (e.g. `track-service.track.uploaded`)
- **Group ID:** `{consumer-service}-{feature}-group` (e.g. `search-service-indexing-group`)
- **DLQ Topic:** `{original-topic}.dlq` (e.g. `track-service.track.uploaded.dlq`)

## 2. Domain Event (domain layer)

- **File path:** `domain/event/{EventName}.java`
- **Rule:** Immutable record, no Spring/Kafka imports.
```java
public record TrackUploadedEvent(
    UUID trackId,
    UUID artistId,
    String title,
    Instant occurredAt
) {}
```

## 3. Publisher Port (application layer)

- **File path:** `application/port/out/{EventName}Publisher.java`
- **Rule:** Interface only — no Kafka dependency.
```java
public interface TrackUploadedPublisher {
    void publish(TrackUploadedEvent event);
}
```

## 4. Kafka Producer (infrastructure layer)

- **File path:** `infrastructure/messaging/Kafka{EventName}Publisher.java`
- **Rule:** Implements publisher port. Serialize sang JSON (Jackson). Include `occurredAt` và `eventType` trong payload.
- **Partition key:** Dùng `entityId` (e.g. `trackId`) để đảm bảo ordering per entity.

## 5. Kafka Consumer (infrastructure layer — consumer service)

- **File path:** `infrastructure/messaging/{EventName}Consumer.java`
- **Rule:** `@KafkaListener` ở đây. Deserialize → gọi Use Case. KHÔNG chứa business logic.
```java
@KafkaListener(topics = "track-service.track.uploaded", groupId = "search-service-indexing-group")
public void consume(TrackUploadedEvent event) {
    indexTrackUseCase.execute(event.trackId());
}
```

## 6. Retry & DLQ Config

- **Retry:** 3 lần với exponential backoff trước khi đẩy sang DLQ.
- **DLQ:** `{original-topic}.dlq` — log đầy đủ payload + exception để debug.
- **Config:** Khai báo trong `infrastructure/config/KafkaConsumerConfig.java`.
- **Rule:** KHÔNG swallow exception — phải log hoặc đẩy DLQ, không được silent fail.