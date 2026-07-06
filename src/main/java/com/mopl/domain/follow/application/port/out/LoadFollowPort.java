package com.mopl.domain.follow.application.port.out;

import com.mopl.domain.follow.domain.Follow;
import java.util.Optional;
import java.util.UUID;

public interface LoadFollowPort {
    Optional<Follow> findById(UUID followId);

    Optional<Follow> findByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId);

    boolean existsByFolloweeIdAndFollowerId(UUID followeeId, UUID followerId);

    long countByFolloweeId(UUID followeeId);
}
