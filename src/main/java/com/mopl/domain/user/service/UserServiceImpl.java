package com.mopl.domain.user.service;

import com.mopl.domain.user.domain.SocialAccount;
import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.*;
import com.mopl.domain.user.event.UserLockedEvent;
import com.mopl.domain.user.event.UserRoleChangedEvent;
import com.mopl.domain.user.mapper.UserMapper;
import com.mopl.domain.user.repository.SocialAccountRepository;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.event.PasswordChangedEvent;
import com.mopl.global.event.UserProfileUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final SocialAccountRepository socialAccountRepository;

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

    @CacheEvict(value = "userSummary", key = "#userId")
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

        Role oldRole = user.getRole();
        user.updateRole(request.role());
        if (oldRole != request.role()) {
            eventPublisher.publishEvent(new UserRoleChangedEvent(user.getId(), oldRole, request.role()));
        }
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

    @Override
    public Optional<UUID> findUserIdBySocialAccount(String provider, String providerId) {
        return socialAccountRepository.findUserIdByProviderAndProviderUserId(provider, providerId);
    }

    @Override
    public UserDto findOrCreateSocialUser(String provider, String providerId, String email, boolean emailVerified, String name, String profileImageUrl) {
        Optional<UUID> existingUserId = findUserIdBySocialAccount(provider, providerId);
        if (existingUserId.isPresent()) {
            User user = userRepository.findById(existingUserId.get())
                    .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));
            if (user.isLocked()) {
                throw new MoplException(ErrorCode.ACCOUNT_LOCKED);
            }
            return userMapper.toDto(user);
        }

        Optional<User> existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            if (provider.equals("google") && emailVerified) {
                User user = existingByEmail.get();

                if (user.isLocked()) {
                    throw new MoplException(ErrorCode.ACCOUNT_LOCKED);
                }
                socialAccountRepository.save(SocialAccount.of(user, provider, providerId));
                return userMapper.toDto(user);
            }
            throw new MoplException(ErrorCode.DUPLICATE_EMAIL);
        }
        String uniqueName = generateUniqueName(name);
        User newUser = User.createOAuthUser(uniqueName, email, Role.USER);
        User savedUser = userRepository.save(newUser);
        linkSocialAccountSafely(savedUser, provider, providerId);
        return userMapper.toDto(savedUser);
    }

    //기존 유저 닉네임과 중복 체크 후 중복 시 뒤에 숫자 붙여 자동 변경
    private String generateUniqueName(String baseName) {
        if (!userRepository.existsByName(baseName)) {
            return baseName;
        }

        int suffix = 1;
        String candidate;
        do {
            candidate = baseName + suffix;
            suffix++;
        } while (userRepository.existsByName(candidate));

        return candidate;
    }
    //동시 요청 시 DataIntegrityViolationException을 catch해서 재조회하는 방식
    private void linkSocialAccountSafely(User user, String provider, String providerId) {
        try {
            socialAccountRepository.save(SocialAccount.of(user, provider, providerId));
        } catch (DataIntegrityViolationException e) {
            boolean alreadyLinked = findUserIdBySocialAccount(provider, providerId)
                    .filter(id -> id.equals(user.getId()))
                    .isPresent();
            if (!alreadyLinked) {
                throw new MoplException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
            }
        }
    }
}