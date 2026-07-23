package com.mopl.domain.user.service;

import com.mopl.domain.user.domain.SocialAccount;
import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.ChangePasswordRequest;
import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.dto.SocialProvider;
import com.mopl.domain.user.dto.UserCreateRequest;
import com.mopl.domain.user.dto.UserLockUpdateRequest;
import com.mopl.domain.user.dto.UserRoleUpdateRequest;
import com.mopl.domain.user.dto.UserUpdateRequest;
import com.mopl.domain.user.event.UserLockedEvent;
import com.mopl.domain.user.event.UserRoleChangedEvent;
import com.mopl.domain.user.repository.SocialAccountRepository;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.event.PasswordChangedEvent;
import com.mopl.global.event.UserProfileUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

// User 엔티티 쓰기(DB 반영 + 이벤트 발행)만 전담하는 트랜잭션 경계.
// S3 presigned URL 변환처럼 DB 커넥션을 붙잡을 필요 없는 작업은 여기서 하지 않고,
// 이 클래스가 반환한 User 엔티티를 UserServiceImpl이 트랜잭션 밖에서 DTO로 변환한다.
@Component
@RequiredArgsConstructor
class UserWriteService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    User register(UserCreateRequest request) {
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

        return userRepository.save(user);
    }

    @Transactional
    User updateProfile(UUID userId, UserUpdateRequest request, String imageUrl) {
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
        return user;
    }

    @Transactional
    User updateRole(UUID userId, UserRoleUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));

        Role oldRole = user.getRole();
        if (oldRole == Role.ADMIN && request.role() != Role.ADMIN
                && !userRepository.existsByRoleAndIdNot(Role.ADMIN, userId)) {
            throw new MoplException(ErrorCode.LAST_ADMIN_CANNOT_BE_CHANGED);
        }

        user.updateRole(request.role());
        if (oldRole != request.role()) {
            eventPublisher.publishEvent(new UserRoleChangedEvent(user.getId(), oldRole, request.role()));
        }
        return user;
    }

    @Transactional
    User updatePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(request.password()));
        eventPublisher.publishEvent(new PasswordChangedEvent(userId));
        return user;
    }

    @Transactional
    User updateLocked(UUID userId, UserLockUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));

        if (request.locked() && user.getRole() == Role.ADMIN
                && !userRepository.existsByRoleAndLockedFalseAndIdNot(Role.ADMIN, userId)) {
            throw new MoplException(ErrorCode.LAST_ADMIN_CANNOT_BE_CHANGED);
        }

        user.updateLocked(request.locked());
        if (request.locked()) {
            eventPublisher.publishEvent(new UserLockedEvent(user.getId()));
        }
        return user;
    }

    @Transactional
    User findOrCreateSocialUser(SocialProvider provider, String providerId, String email, boolean emailVerified, String name, String profileImageUrl) {
        Optional<UUID> existingUserId = socialAccountRepository.findUserIdByProviderAndProviderUserId(provider, providerId);
        if (existingUserId.isPresent()) {
            User user = userRepository.findById(existingUserId.get())
                    .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));
            if (user.isLocked()) {
                throw new MoplException(ErrorCode.ACCOUNT_LOCKED);
            }
            return user;
        }

        Optional<User> existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            if (provider == SocialProvider.GOOGLE && emailVerified) {
                User user = existingByEmail.get();

                if (user.isLocked()) {
                    throw new MoplException(ErrorCode.ACCOUNT_LOCKED);
                }
                linkSocialAccountSafely(user, provider, providerId);
                return user;
            }
            throw new MoplException(ErrorCode.DUPLICATE_EMAIL);
        }
        String uniqueName = generateUniqueName(name);
        User newUser = User.createOAuthUser(uniqueName, email, Role.USER, profileImageUrl);
        User savedUser = userRepository.save(newUser);
        linkSocialAccountSafely(savedUser, provider, providerId);
        return savedUser;
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
    private void linkSocialAccountSafely(User user, SocialProvider provider, String providerId) {
        try {
            socialAccountRepository.save(SocialAccount.of(user, provider, providerId));
        } catch (DataIntegrityViolationException e) {
            boolean alreadyLinked = socialAccountRepository.findUserIdByProviderAndProviderUserId(provider, providerId)
                    .filter(id -> id.equals(user.getId()))
                    .isPresent();
            if (!alreadyLinked) {
                throw new MoplException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
            }
        }
    }
}
