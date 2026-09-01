package com.spotify.auth.infrastructure.integration;

import com.spotify.auth.domain.entity.Role;
import com.spotify.auth.infrastructure.persistence.user.JpaUserRepository;
import com.spotify.auth.infrastructure.persistence.user.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.OffsetDateTime;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataJpaTest xác minh mapping thật JPA entity ↔ Flyway schema (Postgres container).
 *
 * <p>Chạy Flyway migration V1/V2 + insert/query qua Hibernate thật. Bắt lỗi mapping
 * (sai tên cột, sai kiểu, constraint) mà Mockito test không nhìn thấy.
 *
 * <p>@AutoConfigureTestDatabase(NONE): không thay embedded DB — dùng Postgres container.
 *
 * <p>Lưu ý: không gọi setId() thủ công. @GeneratedValue(strategy = UUID) sẽ tự tạo
 * UUID — gọi setId() trước save() bị Hibernate bỏ qua, gây sai ID khi assert.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserJpaRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JpaUserRepository userRepository;

    @Test
    @DisplayName("findByEmail trả về User sau khi save (round-trip Flyway ↔ JPA)")
    void should_FindByEmail_when_UserSaved() {
        UserJpaEntity user = createUser("alice@example.com", "Alice");

        userRepository.save(user);

        var found = userRepository.findByEmail("alice@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
        assertThat(found.get().getDisplayName()).isEqualTo("Alice");
        assertThat(found.get().isVerified()).isTrue();
        assertThat(found.get().getProvider()).isEqualTo("local");
        assertThat(found.get().getRoles()).containsExactly(Role.ROLE_USER);
    }

    @Test
    @DisplayName("existsByEmail true khi email đã tồn tại, false khi không")
    void should_ExistsByEmail_when_EmailPresent() {
        userRepository.save(createUser("bob@example.com", "Bob"));

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    @DisplayName("Audit createdAt/updatedAt tự động set (BaseJpaEntity @CreationTimestamp)")
    void should_SetAuditFields_when_Saved() {
        UserJpaEntity user = createUser("carol@example.com", "Carol");

        userRepository.save(user);
        userRepository.flush();

        UserJpaEntity found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getFailedLoginAttempts()).isZero();
        assertThat(found.isTotpEnabled()).isFalse();
    }

    @Test
    @DisplayName("2FA + profile fields persist đúng (TOTP secret, avatar, lockout)")
    void should_PersistTwoFactorAndProfileFields_when_Saved() {
        OffsetDateTime lockedUntil = OffsetDateTime.now().plusMinutes(5);
        UserJpaEntity user = createUser("dave@example.com", "Dave");
        user.setAvatarUrl("https://example.com/avatar.png");
        user.setTotpSecret("JBSWY3DPEHPK3PXP");
        user.setTotpEnabled(true);
        user.setFailedLoginAttempts(3);
        user.setLockedUntil(lockedUntil);

        userRepository.save(user);
        userRepository.flush();

        UserJpaEntity found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(found.getTotpSecret()).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(found.isTotpEnabled()).isTrue();
        assertThat(found.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(found.getLockedUntil()).isEqualToIgnoringNanos(lockedUntil);
    }

    // === Helper — tạo UserJpaEntity chuẩn với fields tối thiểu, gán ROLE_USER ===
    private UserJpaEntity createUser(String email, String displayName) {
        UserJpaEntity user = new UserJpaEntity();
        user.setEmail(email);
        user.setPassword("$2a$10$hashed");
        user.setDisplayName(displayName);
        user.setRoles(EnumSet.of(Role.ROLE_USER));
        user.setVerified(true);
        user.setProvider("local");
        return user;
    }
}
