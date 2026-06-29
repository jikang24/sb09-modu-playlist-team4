package com.mopl.domain.user.service;

import com.mopl.domain.user.dto.*;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    UserDto register(UserCreateRequest request);

    UserDto find(UUID userId);

    UserDto updateProfile(UUID userId, UserUpdateRequest request, String imageUrl);

    UserDto updateRole(UUID userId, UserRoleUpdateRequest request);

    UserDto updatePassword(UUID userId, ChangePasswordRequest request);

    UserDto updateLocked(UUID userId, UserLockUpdateRequest request);

    Optional<UserAuthInfo> findByEmail(String email);

    Optional<UserAuthInfo> findById(UUID userId);
}
