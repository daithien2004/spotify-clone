---
name: kafka-event
description: Generate Kafka event flow
---

# Skill: kafka-event

Chu trình truyền thông điệp giữa các microservices qua hệ thống sự kiện Kafka:

1. Thiết lập Cấu trúc Topic: `{service-name}.{event-name}`. Group ID: `{consumer-service}-{feature}-group`.
2. Tạo cấu trúc Domain records bất biến: `domain/event/{EventName}.java`.
3. Cùng với output publisher interface `application/port/out/{EventName}Publisher.java`.
4. Implement Infrastructure Kafka Producer tại `infrastructure/messaging/Kafka{EventName}Publisher.java`.
5. Tạo `@KafkaListener` Consumer tại service đích (`infrastructure/messaging/{EventName}Consumer.java`).
6. Cấu hình Retry và DLQ (Dead Letter Queue) topic config nếu có exception trong quá trình tiêu thụ thông điệp.
