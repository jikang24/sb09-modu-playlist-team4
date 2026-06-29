package com.mopl.domain.user.service;

import com.mopl.global.dto.Role;
import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.*;
import com.mopl.domain.user.event.UserLockedEvent;
import com.mopl.domain.user.event.UserRoleChangedEvent;
import com.mopl.domain.user.mapper.UserMapper;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
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
    @Transactional(readOnly = true)
    public Optional<UserAuthInfo> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(u -> new UserAuthInfo(u.getId(), u.getEmail(), u.getPassword(), u.getRole(), u.isLocked()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthInfo> findById(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> new UserAuthInfo(u.getId(), u.getEmail(), u.getPassword(), u.getRole(), u.isLocked()));
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