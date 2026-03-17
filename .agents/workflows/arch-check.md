---
description: Detect Clean Architecture violations in the project
---
# /arch-check

Khi được gọi, quét và phát hiện Architectual Violations:

1. **Spring-leak in Domain**: Quét tìm keyword `org.springframework` trong thư mục `domain/`.
2. **Persistence-leak in Domain**: Quét tìm `JpaRepository`, `@Repository`, `@Entity`, `@Table` trong `domain/`.
3. **Logic leak in Controller**: Tìm controller (REST) có logic xử lý business quá dày (> 3 cấp nested block, xử lý toán học).
4. **God Class**: Tìm Use Case nào đang tiêm (`import`) nhiều hơn 3-4 Dependencies hoặc file > 100 dòng code.
5. In ra 1 báo cáo Artifact dạng thẻ (với Severity `CRITICAL`, `HIGH`, `MEDIUM`) cho từng vi phạm được tìm thấy. Đề xuất lệnh fix.
