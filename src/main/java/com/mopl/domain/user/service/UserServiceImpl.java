package com.mopl.domain.user.service;

import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.*;
import com.mopl.domain.user.event.UserLockedEvent;
import com.mopl.domain.user.event.UserRoleChangedEvent;
import com.mopl.domain.user.mapper.UserMapper;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.event.PasswordChangedEvent;
import com.mopl.global.event.UserProfileUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserDto register(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new MoplException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByName(request.name())) {
            throw new MoplException(ErrorCode.DUPLICATE_NAME);
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .locked(false)
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    public UserDto find(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toDto)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public List<UserDto> findAllByIds(Collection<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(userMapper::toDto)
                .toList();
    }


    @Override
    @Transactional
    public UserDto updateProfile(UUID userId, UserUpdateRequest request, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));

        if (request.name() != null
                && !request.name().equals(user.getName())
                && userRepository.existsByName(request.name())) {
            throw new MoplException(ErrorCode.DUPLICATE_NAME);
        }

        user.updateProfile(request.name(), imageUrl);
        // 프로필(이름/이미지) 변경을 다른 모듈에 알림 - Review가 저장해둔 author 스냅샷을
        // 최신 상태로 맞추기 위해 필요 (이벤트 없이는 리뷰에 옛날 이름이 계속 박제됨)
        eventPublisher.publishEvent(
            new UserProfileUpdatedEvent(user.getId(), user.getName(), imageUrl)
        );
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateRole(UUID userId, UserRoleUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));

        user.updateRole(request.role());
        eventPublisher.publishEvent(new UserRoleChangedEvent(user.getId(), request.role()));
        return userMapper.toDto(user);
    }


    @Override
    @Transactional
    public UserDto updatePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(request.password()));
        eventPublisher.publishEvent(new PasswordChangedEvent(userId));
        return userMapper.toDto(user);
    }


    @Override
    @Transactional
    public UserDto updateLocked(UUID userId, UserLockUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));

        user.updateLocked(request.locked());
        if (request.locked()) {
            eventPublisher.publishEvent(new UserLockedEvent(user.getId()));
        }
        return userMapper.toDto(user);
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
                users.stream().map(userMapper::toDto).toList(),
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                request.sortBy().name(),
                request.sortDirection().name()
        );
    }
}