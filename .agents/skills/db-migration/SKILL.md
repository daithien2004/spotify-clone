---
name: db-migration
description: Create Flyway DB migration and JPA Entity
---

# Skill: db-migration

Quản lý Database Schema Migration bằng Flyway trên nền PostgreSQL:

1. Khởi tạo mã Flyway SQL: `db/migration/V{version}__{table-name}.sql` (ví dụ `V1__init_tracks.sql`).
   - Table name: `snake_case`, danh từ số nhiều. `UUID` default `gen_random_uuid()` PK. `created_at / updated_at` standard. Soft-delete dùng `deleted_at`.
2. Map `JPA Entity`: `infrastructure/persistence/entity/{Entity}JpaEntity.java` có `@Entity(name = "items")`. Fields dùng `@Column` explicit name. Timestamps dùng `@CreationTimestamp`. Không dùng EAGER load reference collection.
3. Map MapStruct: Entity <-> JpaEntity. Data Flow DB không bao giờ tràn thẳng object JPA ra khỏi Infrastructure map.
