package com.mopl.domain.follow.adapter.out.persistence;

import com.mopl.domain.follow.application.port.out.DeleteFollowPort;
import com.mopl.domain.follow.application.port.out.LoadFollowPort;
import com.mopl.domain.follow.application.port.out.SaveFollowPort;
import com.mopl.domain.follow.domain.Follow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowPersistenceAdapter implements SaveFollowPort, LoadFollowPort, DeleteFollowPort {

    private final FollowJpaRepository followJpaRepository;
    private final FollowPersistenceMapper mapper;

    @Override
    @CacheEvict(value = "followerCount", key = "#follow.followeeId")
    public Follow save(Follow follow) {
        return mapper.toDomain(followJpaRepository.save(mapper.toJpaEntity(follow)));
    }

    @Override
    public Optional<Follow> findById(UUID followId) {
        return followJpaRepository.findById(followId).map(mapper::toDomain);
    }

    @Override
    public Optional<Follow> findByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId) {
        return followJpaRepository.findByFolloweeIdAndFollowerId(followeeId, followerId)
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId) {
        return followJpaRepository.existsByFolloweeIdAndFollowerId(followeeId, followerId);
    }

    @Override
    @Cacheable(value = "followerCount", key = "#followeeId")
    public long countByFolloweeId(UUID followeeId) {
        return followJpaRepository.countByFolloweeId(followeeId);
    }

    @Override
    public List<UUID> findFollowerIdsByFolloweeId(UUID followeeId) {
        return followJpaRepository.findFollowerIdsByFolloweeId(followeeId);
    }

    @Override
    @CacheEvict(value = "followerCount", key = "#follow.followeeId")
    public void delete(Follow follow) {
        followJpaRepository.deleteById(follow.getId());
    }
}
