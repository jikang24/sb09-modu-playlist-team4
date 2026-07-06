package com.mopl.domain.follow.dto;

import java.util.UUID;

public record FollowRequest(
        UUID followeeId
) {
}
