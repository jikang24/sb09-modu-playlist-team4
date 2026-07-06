package com.mopl.domain.follow.application.port.in;

import com.mopl.domain.follow.domain.Follow;
import java.util.UUID;

public interface GetFollowedByMeUseCase {
    Follow getFollowedByMe(UUID followeeId, UUID followerId);
}
