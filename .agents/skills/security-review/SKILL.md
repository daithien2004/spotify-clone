---
name: security-review
description: Perform Security Review for Endpoint
---

# Skill: security-review

Checklist bảo mật khi thiết kế hoặc code-review tính năng API:

[ ] Input validation: Chắc chắn request DTO có `@Valid` và message lỗi che mờ.
[ ] SQL injection: Không được connect chuỗi, dùng Map DB Parameter (JPA, NamedParamate).
[ ] JWT Claims: Đọc user id từ session thay vì path params nếu hành động sửa dữ liệu. Role verify.
[ ] Authorization: Đảm bảo resource access phải đối soát với `ownerId` trong context.
[ ] Sensitive: DTO gửi ra UI Không leak internal id, credentials. Password hash không được select default.
[ ] DOS/Rate Limit: API có loop xử lý hay fetch dữ liệu > 100 object ko pagination không?

Phân tách Security Review thành dạng form `PASS/FAIL` cho từng Checkbox mỗi khi xuất Report.
