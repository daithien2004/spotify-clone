package com.spotify.auth.infrastructure.messaging;

import com.spotify.auth.application.port.out.SecurityAuditPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Infrastructure adapter: Publish Security Audit Events ra Kafka.
 * Topic: auth-service.security.audit
 * Consumer: monitoring-service, email-service (gửi cảnh báo), SIEM system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaSecurityAuditPublisher implements SecurityAuditPublisher {

    private static final String TOPIC = "auth-service.security.audit";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Override
    public void publish(String userId, String email, EventType eventType,
                        String ipAddress, String userAgent, String detail) {
        // Audit log luôn được ghi vào application logs (không gian đoạn)
        log.info("[SECURITY_AUDIT] event={} userId={} email={} ip={} ua={} detail={}",
                eventType, userId, email, ipAddress, userAgent, detail);

        if (!kafkaEnabled) {
            // Dev/Test mode: chỉ log, không gửi Kafka
            return;
        }

        Map<String, Object> event = Map.of(
                "eventType", eventType.name(),
                "userId", userId != null ? userId : "unknown",
                "email", email != null ? email : "unknown",
                "ipAddress", ipAddress != null ? ipAddress : "unknown",
                "userAgent", userAgent != null ? userAgent : "unknown",
                "detail", detail != null ? detail : "",
                "occurredAt", Instant.now().toString()
        );

        // Dùng userId làm partition key để đảm bảo ordering per user
        kafkaTemplate.send(TOPIC, userId != null ? userId : "unknown", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // DLQ sẽ xử lý — không swallow exception, chỉ log
                        log.error("[SECURITY_AUDIT] Failed to publish to Kafka: {}", ex.getMessage());
                    }
                });
    }
}
