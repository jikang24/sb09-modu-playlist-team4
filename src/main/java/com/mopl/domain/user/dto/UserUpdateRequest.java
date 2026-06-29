package com.mopl.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank(message = "이름은 필수입니다")
        String name
) {
}
