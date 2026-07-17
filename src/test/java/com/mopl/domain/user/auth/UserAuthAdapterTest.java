package com.mopl.domain.user.auth;

import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.auth.UserAuthInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthAdapter 테스트")
class UserAuthAdapterTest {

    @Mock
    private UserRepository userRepository;

    private UserAuthAdapter adapter;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        adapter = new UserAuthAdapter(userRepository);
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .name("woody")
                .email("woody@mopl.io")
                .password("encoded")
                .profileImageUrl("https://pic.url")
                .role(Role.USER)
                .locked(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("성공: 이메일로 사용자를 찾아 UserAuthInfo로 변환한다")
    void findByEmail_found() {
        when(userRepository.findByEmail("woody@mopl.io")).thenReturn(Optional.of(user));

        Optional<UserAuthInfo> result = adapter.findByEmail("woody@mopl.io");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(userId);
        assertThat(result.get().email()).isEqualTo("woody@mopl.io");
        assertThat(result.get().password()).isEqualTo("encoded");
    }

    @Test
    @DisplayName("실패: 이메일에 해당하는 사용자가 없으면 빈 값을 반환한다")
    void findByEmail_notFound() {
        when(userRepository.findByEmail("unknown@mopl.io")).thenReturn(Optional.empty());

        Optional<UserAuthInfo> result = adapter.findByEmail("unknown@mopl.io");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("성공: id로 사용자를 찾아 UserAuthInfo로 변환한다")
    void findById_found() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Optional<UserAuthInfo> result = adapter.findById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("woody");
        assertThat(result.get().role()).isEqualTo(Role.USER);
        assertThat(result.get().locked()).isFalse();
    }

    @Test
    @DisplayName("실패: id에 해당하는 사용자가 없으면 빈 값을 반환한다")
    void findById_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        Optional<UserAuthInfo> result = adapter.findById(unknownId);

        assertThat(result).isEmpty();
    }
}
