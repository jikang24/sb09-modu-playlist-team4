package com.mopl.domain.follow.application.port.in;

import com.mopl.domain.follow.domain.Follow;
import java.util.UUID;

public interface FollowUserUseCase {
    Follow follow(UUID followeeId, UUID followerId);
}
