package com.mopl.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest (
        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, message = "이름은 최소 2자 이상이어야 합니다.")
        String name,

        @NotBlank @Email
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password
) {
}
