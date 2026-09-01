package com.spotify.auth.infrastructure.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class cho integration tests dùng Postgres thật qua Testcontainers.
 *
 * <p>Mục đích: chạy Flyway migration + JPA mapping/query thật — bắt các lỗi mà
 * Mockito unit test bỏ sót (sai cột, sai constraint, migration mismatch entity).
 * Container dùng chung (static) cho mọi test cùng class -> nhanh, không khởi động
 * lại DB trên từng test method.
 *
 * <p>Note: chỉ override datasource. Redis/Kafka/... vẫn phải mock hoặc excluded
 * bằng slice test (@DataJpaTest/@WebMvcTest) — không dùng @SpringBootTest full context
 * ở đây vì service này còn phụ thuộc Redis + Kafka (không có trong môi trường test).
 */
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    /** Postgres version khớp docker-compose dev (auth_db). Flyway tự tạo schema. */
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_db_test")
            .withUsername("postgres")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
