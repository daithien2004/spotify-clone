---
name: db-migration
description: Create Flyway DB migration and JPA Entity
---

# /db-migration Command

Create Flyway database migrations and JPA entities following project conventions.

## Usage

```
/db-migration create <entity-name>
```

### Arguments
- `entity-name`: The name of the entity to create (e.g., `tracks`, `playlists`, `users`)

## Examples

Create a track entity:
```
/db-migration create tracks
```

Create a playlist entity:
```
/db-migration create playlists
```

Create a user entity:
```
/db-migration create users
```

## What It Creates

The command generates a complete database migration following project standards:

### 1. Flyway Migration SQL
- File: `db/migration/V{version}__{description}.sql` (e.g., `V1_0__create_tracks.sql`)
- Snake_case naming, plural table names (e.g., `tracks`, `playlist_tracks`)
- Includes standard columns:
  ```sql
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at  TIMESTAMPTZ -- soft-delete, NULL = active
  ```
- Never modifies existing migration files (creates new `ALTER` migrations for changes)

### 2. JPA Entity
- File: `infrastructure/persistence/entity/{Entity}JpaEntity.java`
- `@Entity` + `@Table(name = "tracks")` explicit table naming
- `@Column(name = "...", nullable = false)` explicit for all fields
- Timestamps use `@CreationTimestamp` / `@UpdateTimestamp`
- No `@Data` (Lombok) - uses `@Getter @Setter` separately
- No EAGER fetch for collection references
- `equals()`/`hashCode()` overridden based on `id` only

### 3. MapStruct Mapper
- File: `infrastructure/persistence/mapper/{Entity}PersistenceMapper.java`
- `@Mapper(componentModel = "spring")` configuration
- Method naming: `toDomain()` and `toJpaEntity()`
- Enforces data flow rule: JPA Entity never leaks outside infrastructure layer

## Output

The command provides:
- Exact file paths for all generated components
- SQL migration template with proper formatting
- Java entity code with annotations
- Mapper interface implementation
- Version number suggestion for the migration file
- Guidance on when to create U{version}__undo.sql rollback migrations

## Related Commands
- `/kafka-event create` - For generating Kafka event flows for new entities
- `/security-review check` - For performing endpoint security reviews
- `/vercel-react optimize` - For applying frontend performance best practices