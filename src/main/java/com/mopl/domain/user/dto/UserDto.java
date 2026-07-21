package com.mopl.domain.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        Instant createdAt,
        String email,
        String name,
        String profileImageUrl,
        Role role,
        boolean locked
) {
}
