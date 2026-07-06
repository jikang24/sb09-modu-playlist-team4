package com.mopl.domain.follow.application.port.in;

import java.util.UUID;

public interface GetFollowerCountUseCase {
    long countFollowers(UUID followeeId);
}
