---
description: Generate documentation for API, Code, or Flow
---
# /doc

Lệnh `/doc` yêu cầu xuất code document:

1. Nếu gọi vào Java REST Controller: 
   Thêm Swagger/OpenAPI annotations vào đầu class và method (`@Operation`, `@ApiResponse`, `@RequestBody`, `@Parameter`).
2. Nếu gọi vào Domain/Use case:
   Cung cấp cấu trúc JavaDoc chuẩn `@param`, `@return`, `@throws` và `@implNote` cho business rules.
3. Nếu thiết kế flow phức tạp:
   In ra một Mermaid Diagram dạng sequence (`sequenceDiagram`) mô tả giao tiếp giữa Client -> Controller -> UseCase -> EventBroker -> Worker.
