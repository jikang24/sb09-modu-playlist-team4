package com.mopl.domain.user.service;

import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInitializer 테스트")
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        adminInitializer = new AdminInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(adminInitializer, "adminName", "admin");
        ReflectionTestUtils.setField(adminInitializer, "adminEmail", "admin@mopl.io");
        ReflectionTestUtils.setField(adminInitializer, "adminPassword", "admin1234!");
    }

    @Test
    @DisplayName("성공: 관리자 계정이 없으면 새로 생성한다")
    void run_createsAdmin_whenNoneExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("admin1234!")).thenReturn("encoded-password");

        adminInitializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@mopl.io");
        assertThat(captor.getValue().getName()).isEqualTo("admin");
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("성공: 관리자 계정이 이미 있으면 생성을 건너뛴다")
    void run_skipsCreation_whenAdminExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        adminInitializer.run();

        verify(userRepository, never()).save(any(User.class));
    }
}
