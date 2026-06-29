package com.mopl.domain.user.dto;

import com.mopl.global.dto.Role;

import java.util.UUID;

public record UserAuthInfo(
        UUID id,
        String email,
        String password,
        Role role,
        boolean locked
        ) {
}
