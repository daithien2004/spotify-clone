---
name: db-migration
description: Create Flyway DB migration and JPA Entity
---

# Skill: db-migration

Quản lý Database Schema Migration bằng Flyway trên nền PostgreSQL.

## 1. Flyway Migration

- **File path:** `db/migration/V{version}__{description}.sql` (e.g. `V1_0__create_tracks.sql`)
- **Naming:** `snake_case`, danh từ số nhiều (e.g. `tracks`, `playlist_tracks`).
- **Golden Rules:**
  - KHÔNG bao giờ sửa file migration đã chạy — tạo file mới để `ALTER`.
  - Rollback: tạo `U{version}__{description}.sql` nếu cần undo.

- **Standard columns mọi table đều phải có:**
```sql
id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
deleted_at  TIMESTAMPTZ -- soft-delete, NULL = active
```

## 2. JPA Entity

- **File path:** `infrastructure/persistence/entity/{Entity}JpaEntity.java`
- **Annotations:**
  - `@Entity` + `@Table(name = "tracks")` explicit — không để JPA tự đặt tên.
  - `@Column(name = "...", nullable = false)` explicit cho tất cả fields.
  - Timestamps dùng `@CreationTimestamp` / `@UpdateTimestamp`.
  - KHÔNG dùng `@Data` (Lombok) — dùng `@Getter @Setter` riêng.
  - KHÔNG dùng EAGER fetch cho collection references.
- **`equals()`/`hashCode()`:** Override dựa trên `id` — không dùng Lombok `@EqualsAndHashCode`.

## 3. MapStruct Mapper

- **File path:** `infrastructure/persistence/mapper/{Entity}PersistenceMapper.java`
- **Setup:** `@Mapper(componentModel = "spring")`
- **Method naming:** `toDomain()` và `toJpaEntity()`.
- **Data flow rule:** JPA Entity KHÔNG BAO GIỜ tràn ra ngoài `infrastructure/` — luôn map sang Domain Entity trước khi trả về.