package com.spotify.user.infrastructure.persistence.adapter;

import com.spotify.user.domain.entity.User;
import com.spotify.user.domain.repository.UserRepository;
import com.spotify.user.infrastructure.persistence.entity.UserJpaEntity;
import com.spotify.user.infrastructure.persistence.mapper.UserJpaMapper;
import com.spotify.user.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaRepository;
    private final UserJpaMapper mapper;

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomainEntity);
    }

    @Override
    public List<User> findByIds(List<UUID> ids) {
        // Map theo id rồi nối theo thứ tự input — giữ thứ tự followers/following list.
        Map<UUID, User> byId = jpaRepository.findAllByIdInOrder(ids).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return ids.stream().map(byId::get).toList();
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = jpaRepository.save(mapper.toJpaEntity(user));
        return mapper.toDomainEntity(saved);
    }
}
