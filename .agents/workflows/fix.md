---
description: Fix a reported bug or error message
---
# /fix [error-message]

Khi user nhập `/fix` cùng mã lỗi / mô tả bug:

1. Đọc và nhận dạng Error message để hiểu:
   - Bug xuất phát từ Layer nào? (Domain, Application, hay Presentation layer?)
   - Root Cause là gì? 
   - Có vi phạm luật Clean Architecture nào không? (ví dụ: Controller chọc trực tiếp DB).
2. Lên plan propose một "Fix tối thiểu" (không over-engineer code).
3. Đề xuất Root Cause + Script sửa chữa.
4. (Optional) Hỏi người dùng xem có muốn ứng dụng TDD (Test-Driven Development) để thiết lập Regression Test trước rồi mới implement fix code không.
