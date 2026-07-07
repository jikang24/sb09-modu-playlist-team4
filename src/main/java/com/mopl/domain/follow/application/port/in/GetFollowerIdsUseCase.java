package com.mopl.domain.follow.application.port.in;

import java.util.List;
import java.util.UUID;

public interface GetFollowerIdsUseCase {
    List<UUID> getFollowerIds(UUID followeeId);
}
