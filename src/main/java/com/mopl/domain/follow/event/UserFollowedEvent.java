package com.mopl.domain.follow.event;

import java.util.UUID;

public record UserFollowedEvent(
    UUID followeeId,
    UUID followerId
) {
}
