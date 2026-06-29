package com.mopl.domain.auth.dto;

public record SignInRequest(
        String email,
        String password
) {
}
