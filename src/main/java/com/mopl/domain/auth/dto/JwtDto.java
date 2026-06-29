package com.mopl.domain.auth.dto;

import com.mopl.domain.user.dto.UserDto;
import jakarta.validation.constraints.NotNull;

public record JwtDto(
        @NotNull
        UserDto userDto,

        @NotNull
        String accessToken
) {
}
