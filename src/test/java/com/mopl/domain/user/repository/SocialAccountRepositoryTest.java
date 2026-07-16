package com.mopl.domain.user.repository;

import com.mopl.domain.user.domain.SocialAccount;
import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.dto.SocialProvider;
import com.mopl.global.config.JpaConfig;
import com.mopl.global.config.QueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import({QueryDslConfig.class, JpaConfig.class})
@DisplayName("SocialAccountRepository 테스트")
class SocialAccountRepositoryTest {

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = User.createOAuthUser("woody", "woody@mopl.io", Role.USER, null);
        savedUser = userRepository.save(user);
    }

    @Test
    @DisplayName("성공: provider와 providerUserId로 userId를 조회한다")
    void findUserIdByProviderAndProviderUserId_found() {
        socialAccountRepository.save(SocialAccount.of(savedUser, SocialProvider.GOOGLE, "google-1"));

        Optional<UUID> result = socialAccountRepository
                .findUserIdByProviderAndProviderUserId(SocialProvider.GOOGLE, "google-1");

        assertThat(result).contains(savedUser.getId());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 provider/providerUserId 조합이면 빈 값을 반환한다")
    void findUserIdByProviderAndProviderUserId_notFound() {
        Optional<UUID> result = socialAccountRepository
                .findUserIdByProviderAndProviderUserId(SocialProvider.GOOGLE, "unknown");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("실패: 같은 providerUserId라도 provider가 다르면 조회되지 않는다")
    void findUserIdByProviderAndProviderUserId_differentProvider() {
        socialAccountRepository.save(SocialAccount.of(savedUser, SocialProvider.GOOGLE, "shared-id"));

        Optional<UUID> result = socialAccountRepository
                .findUserIdByProviderAndProviderUserId(SocialProvider.KAKAO, "shared-id");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("성공: 동일 provider에 동일 providerUserId를 중복 저장하면 제약 조건 위반이 발생한다")
    void uniqueConstraint_violatedOnDuplicate() {
        socialAccountRepository.save(SocialAccount.of(savedUser, SocialProvider.GOOGLE, "dup-id"));
        socialAccountRepository.flush();

        User anotherUser = userRepository.save(User.createOAuthUser("buzz", "buzz@mopl.io", Role.USER, null));

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            socialAccountRepository.save(SocialAccount.of(anotherUser, SocialProvider.GOOGLE, "dup-id"));
            socialAccountRepository.flush();
        });
    }
}
