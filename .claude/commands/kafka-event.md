---
name: kafka-event
description: Generate Kafka event flow between microservices
---

# /kafka-event Command

Generate Kafka event flows between microservices following Clean Architecture principles.

## Usage

```
/kafka-event create <event-name>
```

### Arguments
- `event-name`: The name of the domain event to create (e.g., `track-uploaded`, `playlist-track-added`)

## Examples

Create a track uploaded event:
```
/kafka-event create track-uploaded
```

Create a playlist track added event:
```
/kafka-event create playlist-track-added
```

Create a user followed event:
```
/kafka-event create user-followed
```

## What It Generates

The command creates a complete Kafka event flow including:

### 1. Topic & Group Naming
- Topic: `{service-name}.{entity}.{event}` (e.g., `track-service.track.uploaded`)
- Group ID: `{consumer-service}-{feature}-group` (e.g., `search-service-indexing-group`)
- DLQ Topic: `{original-topic}.dlq` (e.g., `track-service.track.uploaded.dlq`)

### 2. Domain Event (domain layer)
- File: `domain/event/{EventName}.java`
- Immutable record with no Spring/Kafka imports
- Includes `occurredAt` timestamp

### 3. Publisher Port (application layer)
- File: `application/port/out/{EventName}Publisher.java`
- Interface only — no Kafka dependency

### 4. Kafka Producer (infrastructure layer)
- File: `infrastructure/messaging/Kafka{EventName}Publisher.java`
- Implements publisher port
- Serializes to JSON (Jackson)
- Includes `occurredAt` and `eventType` in payload
- Uses `entityId` as partition key for ordering

### 5. Kafka Consumer (infrastructure layer)
- File: `infrastructure/messaging/{EventName}Consumer.java`
- `@KafkaListener` annotation
- Deserializes payload → calls Use Case
- Contains no business logic

### 6. Retry & DLQ Configuration
- 3 retry attempts with exponential backoff
- DLQ topic for failed messages
- Full payload + exception logging for debugging
- No silent exception swallowing

## Output

The command provides:
- Complete file paths for all generated components
- Code snippets for each layer
- Topic naming conventions
- Configuration guidance
- Testing recommendations

## Related Commands
- `/db-migration create` - For creating database schema changes
- `/security-review check` - For performing endpoint security reviews
- `/vercel-react optimize` - For applying frontend performance best practices