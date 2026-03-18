---
name: security-review
description: Perform Security Review for Endpoint
---

# Skill: security-review

Checklist bảo mật khi thiết kế hoặc review API endpoint. Xuất report dạng PASS/FAIL cho từng mục.

## Checklist

### 🔵 Input & Validation
- [ ] Request DTO có `@Valid` và Bean Validation annotations.
- [ ] Error messages không leak internal detail (stack trace, field names, DB info).
- [ ] Không concat chuỗi SQL — dùng JPA named parameters hoặc `@Query` với binding.
- [ ] File upload (nếu có): validate MIME type, size limit, scan filename cho path traversal.

### 🔵 Authentication & Authorization
- [ ] Endpoint có `@PreAuthorize` hoặc Security Filter — không để accidentally public.
- [ ] User identity đọc từ JWT claims (`SecurityContext`), KHÔNG từ request body hay path param.
- [ ] Resource access đối soát `ownerId` — user A không được đọc/sửa data của user B.
- [ ] Role/Permission verify đúng cấp (e.g. `ROLE_ARTIST` mới được upload track).

### 🔵 Sensitive Data
- [ ] Response DTO không leak: `password`, `passwordHash`, internal `UUID` không cần thiết, `stackTrace`.
- [ ] Password không được `SELECT` theo mặc định — dùng `@JsonIgnore` hoặc projection riêng.
- [ ] Thông tin nhạy cảm trong log phải được mask (email, token, card number).

### 🔵 Performance & DOS
- [ ] Endpoint có loop hoặc fetch > 100 objects phải có pagination.
- [ ] Rate limiting được áp dụng tại API Gateway cho endpoint public.
- [ ] Query N+1 không xuất hiện — kiểm tra với Hibernate `show_sql` hoặc p6spy.

### 🔵 Kafka / Async (nếu có)
- [ ] Consumer không silent fail — exception phải log hoặc đẩy DLQ.
- [ ] Event payload không chứa sensitive data dạng plain text.

---

## Report Template

Khi review xong, xuất report theo format sau:

| # | Check | Status | Notes |
|---|-------|--------|-------|
| 1 | Input validation | ✅ PASS / ❌ FAIL | |
| 2 | No SQL injection | ✅ PASS / ❌ FAIL | |
| 3 | JWT claims only | ✅ PASS / ❌ FAIL | |
| 4 | Ownership check | ✅ PASS / ❌ FAIL | |
| 5 | No sensitive leak | ✅ PASS / ❌ FAIL | |
| 6 | Pagination / rate limit | ✅ PASS / ❌ FAIL | |
| 7 | Sensitive data in logs | ✅ PASS / ❌ FAIL | |
| 8 | Kafka safe (if applicable) | ✅ PASS / ❌ FAIL | N/A nếu không có |

**Overall:** ✅ PASS / ❌ FAIL — [summary ngắn nếu có FAIL]