package com.mopl.domain.user.dto;

import com.mopl.global.dto.Role;

public record UserRoleUpdateRequest (
        Role role
){
}
