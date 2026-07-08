package com.mopl.domain.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest (
        @NotNull Role role
        ){
}
