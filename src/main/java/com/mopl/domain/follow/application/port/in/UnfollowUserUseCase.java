package com.mopl.domain.follow.application.port.in;

import java.util.UUID;

public interface UnfollowUserUseCase {
    void unfollow(UUID followId, UUID requesterId);
}
