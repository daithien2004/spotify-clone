---
name: new-usecase
description: Create a new feature use-case following Clean Architecture
---

# Skill: new-usecase

Chu trình tạo chuẩn Clean Architecture Use-case cho hệ thống Spotify Clone:

1. Thu thập tên Feature (vd: UploadTrack) và Tên Microservice (vd: track-service).
2. Viết file theo đúng thứ tự Data Flow:
   - `domain/entity/{Entity}.java` (không Spring)
   - `domain/repository/{Entity}Repository.java` (Domain Interface)
   - `domain/event/{Feature}Event.java` (Event Contract record)
   - `application/port/in/{Feature}UseCase.java` (Input port)
   - `application/port/in/{Feature}Command.java` (Payload Object)
   - `application/port/out/{Feature}Result.java` (Return view)
   - `application/usecase/{Feature}UseCaseImpl.java` (Impl)
   - `infrastructure/persistence/entity/{Entity}JpaEntity.java`
   - `infrastructure/persistence/repository/Jpa{Entity}Repository.java`
   - `infrastructure/persistence/{Entity}RepositoryAdapter.java` (Mapping Domain <-> JPA)
   - `presentation/controller/{Entity}Controller.java` (Gọi Use case)
3. Luôn luôn viết Unit Test với Mockito cho `UseCaseImpl` gồm (Happy path + Edge cases).
