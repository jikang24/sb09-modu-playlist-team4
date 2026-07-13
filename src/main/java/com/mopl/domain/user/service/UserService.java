package com.mopl.domain.user.service;

import com.mopl.domain.user.dto.*;
import com.mopl.global.response.CursorPageResponse;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    UserDto register(UserCreateRequest request);

    UserDto find(UUID userId);

    /** 존재하는 id만 반환 - 목록 조회에서 유저 정보를 배치로 붙일 때 씀 (N+1 방지용) */
    List<UserDto> findAllByIds(Collection<UUID> userIds);

    UserDto updateProfile(UUID userId, UserUpdateRequest request, String imageUrl);

    UserDto updateRole(UUID userId, UserRoleUpdateRequest request);

    UserDto updatePassword(UUID userId, ChangePasswordRequest request);

    UserDto updateLocked(UUID userId, UserLockUpdateRequest request);

    CursorPageResponse<UserDto> findAll(UserSearchRequest request);

    Optional<UUID> findUserIdBySocialAccount(SocialProvider provider, String providerId);

    UserDto findOrCreateSocialUser(SocialProvider provider, String providerId, String email, boolean emailVerified, String name, String profileImageUrl);

}
