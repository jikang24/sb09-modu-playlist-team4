package com.mopl.domain.user.service;

import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.*;
import com.mopl.domain.user.mapper.UserMapper;
import com.mopl.domain.user.repository.SocialAccountRepository;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// DB 쓰기(트랜잭션)는 UserWriteService에 위임하고, 여기서는 그 결과를 DTO로 변환한다.
// toDto()의 S3 presigned URL 변환은 네트워크 호출이라, 트랜잭션이 끝난 뒤 수행되도록 트랜잭션 경계와 분리했다.
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SocialAccountRepository socialAccountRepository;
    private final ProfileImageUrlResolver profileImageUrlResolver;
    private final UserWriteService userWriteService;

    private UserDto toDto(User user) {
        return resolveImageUrl(userMapper.toDto(user));
    }

    private UserDto resolveImageUrl(UserDto dto) {
        String presignedUrl = profileImageUrlResolver.resolve(dto.profileImageUrl());
        return new UserDto(dto.id(), dto.createdAt(), dto.email(), dto.name(), presignedUrl, dto.role(), dto.locked());
    }

    @Override
    public UserDto register(UserCreateRequest request) {
        User savedUser = userWriteService.register(request);
        return toDto(savedUser);
    }

    @Override
    public UserDto find(UUID userId) {
        return userRepository.findById(userId)
                .map(this::toDto)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public UserDto findRaw(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toDto)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public List<UserDto> findAllByIds(Collection<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(this::toDto)
                .toList();
    }

    @CacheEvict(value = "userSummary", key = "#userId")
    @Override
    public UserDto updateProfile(UUID userId, UserUpdateRequest request, String imageUrl) {
        User user = userWriteService.updateProfile(userId, request, imageUrl);
        return toDto(user);
    }

    @Override
    public UserDto updateRole(UUID userId, UserRoleUpdateRequest request) {
        User user = userWriteService.updateRole(userId, request);
        return toDto(user);
    }

    @Override
    public UserDto updatePassword(UUID userId, ChangePasswordRequest request) {
        User user = userWriteService.updatePassword(userId, request);
        return toDto(user);
    }

    @Override
    public UserDto updateLocked(UUID userId, UserLockUpdateRequest request) {
        User user = userWriteService.updateLocked(userId, request);
        return toDto(user);
    }

    @Override
    public CursorPageResponse<UserDto> findAll(UserSearchRequest request) {

        List<User> users = userRepository.findAllWithCursor(request);

        boolean hasNext = users.size() > request.limit();
        if (hasNext) {
            users = users.subList(0, request.limit());
        }

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !users.isEmpty()) {
            User last = users.get(users.size() - 1);
            nextIdAfter = last.getId();
            nextCursor = switch (request.sortBy()) {
                case NAME -> last.getName();
                case EMAIL -> last.getEmail();
                case CREATEDAT -> last.getCreatedAt().toString();
                case ISLOCKED -> String.valueOf(last.isLocked());
                case ROLE -> last.getRole().name();
            };
        }
        long totalCount = userRepository.countAll(request);
        return new CursorPageResponse<>(
                users.stream().map(this::toDto).toList(),
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                request.sortBy().name(),
                request.sortDirection().name()
        );
    }

    @Override
    public Optional<UUID> findUserIdBySocialAccount(SocialProvider provider, String providerId) {
        return socialAccountRepository.findUserIdByProviderAndProviderUserId(provider, providerId);
    }

    @Override
    public UserDto findOrCreateSocialUser(SocialProvider provider, String providerId, String email, boolean emailVerified, String name, String profileImageUrl) {
        User user = userWriteService.findOrCreateSocialUser(provider, providerId, email, emailVerified, name, profileImageUrl);
        return toDto(user);
    }
}
