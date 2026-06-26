package com.mopl.domain.user.event;

import com.mopl.domain.user.domain.Role;

import java.util.UUID;

public record UserRoleChangedEvent(
        UUID userId, Role newRole
) {
}
