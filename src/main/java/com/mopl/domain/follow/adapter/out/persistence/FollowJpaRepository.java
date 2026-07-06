package com.mopl.domain.follow.adapter.out.persistence;

import com.mopl.domain.follow.adapter.out.FollowJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowJpaRepository extends JpaRepository<FollowJpaEntity, UUID> {
    Optional<FollowJpaEntity> findByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId);

    boolean existsByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId);

    long countByFolloweeId(UUID followeeId);
}
