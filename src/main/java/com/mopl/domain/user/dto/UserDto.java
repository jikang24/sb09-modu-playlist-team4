package com.mopl.domain.user.dto;

import com.mopl.domain.user.domain.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        LocalDateTime createdAt,
        String email,
        String name,
        String profileImageUrl,
        Role role,
        boolean locked
) {
}
