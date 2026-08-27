---
name: security-review
description: Perform security review for API endpoints
---

# /security-review Command

Perform a security review for API endpoints using the security-review skill.

## Usage

```
/security-review check <endpoint-path>
```

### Arguments
- `endpoint-path`: The API endpoint path to review (e.g., `/api/tracks`, `/api/users/{id}`)

## Examples

Review a specific endpoint:
```
/security-review check /api/tracks/{trackId}
```

Review a collection endpoint:
```
/security-review check /api/playlists
```

Review an auth endpoint:
```
/security-review check /api/auth/refresh
```

## What It Checks

The command performs a comprehensive security review including:

### 🔵 Input & Validation
- Request DTO validation with `@Valid` and Bean Validation
- Error messages that don't leak internal details
- No SQL string concatenation
- File upload validation (MIME type, size, path traversal)

### 🔵 Authentication & Authorization
- Endpoint protection with `@PreAuthorize` or Security Filter
- User identity from JWT claims (not request body/path)
- Resource access ownership checks (`ownerId` verification)
- Proper role/permission verification

### 🔵 Sensitive Data
- Response DTO sanitization (no passwords, passwordHash, etc.)
- Password field exclusion from SELECT queries
- Sensitive data masking in logs

### 🔵 Performance & DOS
- Pagination for large result sets
- Rate limiting verification (at API Gateway)
- N+1 query prevention

### 🔵 Kafka / Async (if applicable)
- Proper exception handling in consumers (no silent fails)
- Safe event payloads (no sensitive plain text)

## Output

The command generates a PASS/FAIL report for each checklist item with an overall assessment and summary if any items fail.

## Related Commands
- `/db-migration create` - For creating database schema changes
- `/kafka-event create` - For generating Kafka event flows
- `/vercel-react optimize` - For applying frontend performance best practices