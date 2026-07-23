package com.mopl.domain.user.auth;

import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.domain.user.service.ProfileImageUrlResolver;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.auth.UserAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserAuthAdapter implements UserAuthPort {

    private final UserRepository userRepository;
    private final ProfileImageUrlResolver profileImageUrlResolver;

    // 조회(userRepository 호출) 자체는 Spring Data JPA가 개별적으로 트랜잭션을 걸어준다.
    // presign(S3 네트워크 호출)을 트랜잭션 안에 묶으면 그 시간만큼 DB 커넥션을 붙잡게 되어
    // HikariCP leak-detection이 발동하므로, 여기서는 일부러 @Transactional을 걸지 않고
    // 조회 트랜잭션이 끝난 뒤에 presign을 수행한다.
    @Override
    public Optional<UserAuthInfo> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toAuthInfo);
    }

    @Override
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