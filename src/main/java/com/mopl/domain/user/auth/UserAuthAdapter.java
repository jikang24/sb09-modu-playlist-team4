package com.mopl.domain.user.auth;

import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.domain.user.service.ProfileImageUrlResolver;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.auth.UserAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserAuthAdapter implements UserAuthPort {

    private final UserRepository userRepository;
    private final ProfileImageUrlResolver profileImageUrlResolver;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthInfo> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toAuthInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthInfo> findById(UUID userId) {
        return userRepository.findById(userId).map(this::toAuthInfo);
    }

    private UserAuthInfo toAuthInfo(User user) {
        return new UserAuthInfo(
                user.getId(),
                user.getCreatedAt(),
                user.getEmail(),
                user.getPassword(),
                user.getName(),
                profileImageUrlResolver.resolve(user.getProfileImageUrl()),
                user.getRole(),
                user.isLocked()
        );
    }
}