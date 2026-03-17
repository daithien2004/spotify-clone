---
description: Refactor given class, module, or snippet
---
# /refactor

Khi user yêu cầu `/refactor` một tính năng hay file nhất định:

1. Sử dụng codebase search để tìm file.
2. Kiểm tra code theo các rule:
   - Có đúng Clean Architecture layer không?
   - Có tuân thủ SOLID (đặc biệt Single Responsibility & Dependency Inversion)?
   - Chuẩn Naming của `CONVENTIONS.md`?
   - Có nguy cơ N+1 query không?
3. Sau khi xác định vấn đề, xuất diff file hoặc danh sách file thay đổi kèm annotation/comment tại sao refactor.
4. Đợi người dùng approve trước khi write file thật.
