package com.mopl.domain.user.dto;

public record UserCreateRequest (
        String name,
        String email,
        String password
) {
}
