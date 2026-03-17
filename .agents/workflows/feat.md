---
description: Create a new feature end-to-end (Clean Architecture)
---
# /feat [tên-feature]

Khi user gõ `/feat` kèm tên tính năng (Ví dụ: `/feat upload-track`):

1. Auto-fetch các rule từ `CONTEXT.md`, `DOMAIN.md`, `CONVENTIONS.md`.
2. Suy nghĩ và phân tích tính năng:
   - Domain entities bị ảnh hưởng?
   - Tên Use case chuẩn Convention?
   - API endpoints cần thiết?
   - Frontend page / UX component?
   - Flow DB mapping hay Kafka event?
3. In ra danh sách các file **sẽ được sinh ra** theo chuẩn layers Clean Architecture.
4. Tạm ngưng và yêu cầu xác nhận `Bạn có đồng ý tạo toàn bộ cấu trúc file trên không?`
5. Nếu Yes: Generate các file theo thứ tự đúng của DDD:
   - DB Migration
   - Domain Event / Entity
   - Domain Repository Interface
   - Application Use-case Interface & Impl
   - Infrastructure JPA entity, Mapper, Kafka, Repository Impl
   - Presentation Controller, Request/Response DTO
   - Frontend components & hooks
   - Cập nhật file `PROGRESS.md` sang trạng thái In-Progress.
