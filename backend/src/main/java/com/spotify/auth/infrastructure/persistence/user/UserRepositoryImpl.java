package com.spotify.auth.infrastructure.persistence.user;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.repository.UserRepository;
import com.spotify.auth.domain.valueobject.Email;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final JpaUserRepository jpaRepository;
    private final UserJpaMapper mapper;
    private final EntityManager entityManager;

    @Override
    @CacheEvict(value = "users", key = "#user.email.value()")
    public User save(User user) {
        UserJpaEntity entity = mapper.toJpaEntity(user);
        entity = entityManager.merge(entity);
        return mapper.toDomainEntity(entity);
    }

    @Override
    @Cacheable(value = "users", key = "#email.value()")
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value())
                .map(mapper::toDomainEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomainEntity);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }
}
