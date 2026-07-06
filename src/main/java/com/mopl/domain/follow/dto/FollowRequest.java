package com.mopl.domain.follow.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FollowRequest(
        @NotNull(message = "팔로우할 사용자 ID는 필수입니다")
        UUID followeeId
) {
}
