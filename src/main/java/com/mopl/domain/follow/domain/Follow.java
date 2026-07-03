package com.mopl.domain.follow.domain;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class Follow {
    private final UUID id;
    private final UUID followerId;
    private final UUID followeeId;
    private final Instant createdAt;

    public static Follow create(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new MoplException(ErrorCode.CANNOT_FOLLOW_SELF);
        }
        return Follow.builder()
                .id(UUID.randomUUID())
                .followerId(followerId)
                .followeeId(followeeId)
                .createdAt(Instant.now())
                .build();
    }
}
