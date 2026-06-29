package com.mopl.domain.user.event;

import com.mopl.global.dto.Role;

import java.util.UUID;

public record UserRoleChangedEvent(
        UUID userId, Role newRole
) {
}
