package com.mopl.domain.user.dto;

import com.mopl.domain.user.domain.Role;

public record UserRoleUpdateRequest (
        Role role
){
}
