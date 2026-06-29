package com.mopl.domain.user.dto;

import com.mopl.domain.user.domain.Role;
import com.mopl.global.dto.SortDirection;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserSearchRequest(
        String emailLike,
        Role roleEqual,
        Boolean isLocked,
        String cursor,
        UUID idAfter,
        @NotNull Integer limit,
        @NotNull SortDirection sortDirection,
        @NotNull UserSortBy sortBy
) {}

