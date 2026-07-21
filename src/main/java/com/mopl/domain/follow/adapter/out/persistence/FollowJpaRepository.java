package com.mopl.domain.follow.adapter.out.persistence;

import com.mopl.domain.follow.adapter.out.FollowJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FollowJpaRepository extends JpaRepository<FollowJpaEntity, UUID> {
    Optional<FollowJpaEntity> findByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId);

    boolean existsByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId);

    long countByFolloweeId(UUID followeeId);

    @Query("select f.followerId from FollowJpaEntity f where f.followeeId = :followeeId")
    List<UUID> findFollowerIdsByFolloweeId(UUID followeeId);
}
