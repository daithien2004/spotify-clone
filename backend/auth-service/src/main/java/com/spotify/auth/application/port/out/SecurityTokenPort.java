package com.spotify.auth.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Port để lưu và đọc các token dùng một lần (One-time-use tokens):
 * - Email Verification Token
 * - Password Reset Token
 * Lưu trong Redis với TTL tự động hết hạn.
 */
public interface SecurityTokenPort {

    /** Lưu token với type và TTL (giây) vào Redis */
    void save(String token, UUID userId, String tokenType, long ttlSeconds);

    /** Lấy userId gắn với token (trả về empty nếu token hết hạn hoặc không tồn tại) */
    Optional<UUID> findUserIdByToken(String token, String tokenType);

    /** Xoá token sau khi đã dùng (để ngăn dùng lại) */
    void delete(String token, String tokenType);
}
