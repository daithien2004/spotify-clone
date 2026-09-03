package com.spotify.user.infrastructure.persistence.adapter;

import com.spotify.user.domain.entity.Follow;
import com.spotify.user.domain.repository.FollowRepository;
import com.spotify.user.infrastructure.persistence.mapper.FollowJpaMapper;
import com.spotify.user.infrastructure.persistence.repository.JpaFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FollowRepositoryImpl implements FollowRepository {

    private final JpaFollowRepository jpaRepository;
    private final FollowJpaMapper mapper;

    @Override
    public Optional<Follow> findFollowing(UUID followerId, UUID followeeId) {
        return jpaRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .map(mapper::toDomainEntity);
    }

    @Override
    public List<Follow> findAllByFollower(UUID followerId) {
        return jpaRepository.findAllByFollowerId(followerId).stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public List<Follow> findAllByFollowee(UUID followeeId) {
        return jpaRepository.findAllByFolloweeId(followeeId).stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public boolean exists(UUID followerId, UUID followeeId) {
        return jpaRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Override
    public void save(Follow follow) {
        jpaRepository.save(mapper.toJpaEntity(follow));
    }

    @Override
    public void delete(Follow follow) {
        jpaRepository.deleteById(follow.getId());
    }
}
