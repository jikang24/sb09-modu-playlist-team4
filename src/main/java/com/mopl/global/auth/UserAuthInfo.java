package com.mopl.global.auth;

import com.mopl.global.dto.Role;

import java.time.Instant;
import java.util.UUID;

public record UserAuthInfo(
        UUID id,
        Instant createdAt,
        String email,
        String password,
        String name,
        String profileImageUrl,
        Role role,
        boolean locked
) {}
