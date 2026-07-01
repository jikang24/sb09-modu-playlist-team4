package com.mopl.domain.user.domain;

import com.mopl.domain.user.dto.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User 엔티티 테스트")
class UserTest {

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .name("woody")
                .email("woody@mopl.io")
                .password("encodedPassword")
                .profileImageUrl("https://example.com/image.png")
                .role(Role.USER)
                .locked(false)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("updateProfile: 프로필 수정")
    class UpdateProfile {

        @Test
        @DisplayName("성공: 이름과 이미지 URL이 모두 업데이트된다")
        void updateProfile_both() {
            user.updateProfile("buzz", "https://example.com/new-image.png");

            assertThat(user.getName()).isEqualTo("buzz");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/new-image.png");
        }

        @Test
        @DisplayName("성공: 이름만 null이면 이름만 업데이트되지 않는다")
        void updateProfile_nameNull() {
            String originalName = user.getName();
            user.updateProfile(null, "https://example.com/new-image.png");

            assertThat(user.getName()).isEqualTo(originalName);
            assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/new-image.png");
        }

        @Test
        @DisplayName("성공: 이미지 URL이 null이면 URL만 업데이트되지 않는다")
        void updateProfile_imageUrlNull() {
            String originalImageUrl = user.getProfileImageUrl();
            user.updateProfile("buzz", null);

            assertThat(user.getName()).isEqualTo("buzz");
            assertThat(user.getProfileImageUrl()).isEqualTo(originalImageUrl);
        }

        @Test
        @DisplayName("성공: 둘 다 null이면 변경되지 않는다")
        void updateProfile_both_null() {
            String originalName = user.getName();
            String originalImageUrl = user.getProfileImageUrl();

            user.updateProfile(null, null);

            assertThat(user.getName()).isEqualTo(originalName);
            assertThat(user.getProfileImageUrl()).isEqualTo(originalImageUrl);
        }

        @Test
        @DisplayName("성공: 빈 문자열로 업데이트된다")
        void updateProfile_emptyString() {
            user.updateProfile("", "");

            assertThat(user.getName()).isEmpty();
            assertThat(user.getProfileImageUrl()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateRole: 권한 수정")
    class UpdateRole {

        @Test
        @DisplayName("성공: USER에서 ADMIN으로 변경된다")
        void updateRole_userToAdmin() {
            assertThat(user.getRole()).isEqualTo(Role.USER);

            user.updateRole(Role.ADMIN);

            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("성공: ADMIN에서 USER로 변경된다")
        void updateRole_adminToUser() {
            user.updateRole(Role.ADMIN);
            assertThat(user.getRole()).isEqualTo(Role.ADMIN);

            user.updateRole(Role.USER);

            assertThat(user.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("성공: 같은 권한으로 업데이트해도 문제없다")
        void updateRole_same() {
            user.updateRole(Role.USER);

            assertThat(user.getRole()).isEqualTo(Role.USER);
        }
    }

    @Nested
    @DisplayName("updatePassword: 비밀번호 변경")
    class UpdatePassword {

        @Test
        @DisplayName("성공: 비밀번호가 변경된다")
        void updatePassword_success() {
            String originalPassword = user.getPassword();
            String newEncodedPassword = "newEncodedPassword123";

            user.updatePassword(newEncodedPassword);

            assertThat(user.getPassword()).isEqualTo(newEncodedPassword);
            assertThat(user.getPassword()).isNotEqualTo(originalPassword);
        }

        @Test
        @DisplayName("성공: 긴 암호화된 비밀번호로 업데이트된다")
        void updatePassword_longPassword() {
            String longEncodedPassword = "$2a$10$abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqr";

            user.updatePassword(longEncodedPassword);

            assertThat(user.getPassword()).isEqualTo(longEncodedPassword);
        }

        @Test
        @DisplayName("성공: null로 업데이트할 수 있다")
        void updatePassword_null() {
            user.updatePassword(null);

            assertThat(user.getPassword()).isNull();
        }
    }

    @Nested
    @DisplayName("updateLocked: 계정 잠금 상태 변경")
    class UpdateLocked {

        @Test
        @DisplayName("성공: locked=false에서 true로 변경된다")
        void updateLocked_false_to_true() {
            assertThat(user.isLocked()).isFalse();

            user.updateLocked(true);

            assertThat(user.isLocked()).isTrue();
        }

        @Test
        @DisplayName("성공: locked=true에서 false로 변경된다")
        void updateLocked_true_to_false() {
            user.updateLocked(true);
            assertThat(user.isLocked()).isTrue();

            user.updateLocked(false);

            assertThat(user.isLocked()).isFalse();
        }

        @Test
        @DisplayName("성공: 같은 상태로 업데이트해도 문제없다")
        void updateLocked_same_false() {
            user.updateLocked(false);

            assertThat(user.isLocked()).isFalse();
        }

        @Test
        @DisplayName("성공: locked=true 상태에서 true로 설정해도 문제없다")
        void updateLocked_same_true() {
            user.updateLocked(true);
            user.updateLocked(true);

            assertThat(user.isLocked()).isTrue();
        }
    }

    @Nested
    @DisplayName("필드 초기값 테스트")
    class DefaultValues {

        @Test
        @DisplayName("성공: 새로 생성된 사용자는 locked=false이다")
        void defaultLocked_false() {
            User newUser = User.builder()
                    .name("test")
                    .email("test@mopl.io")
                    .password("password")
                    .role(Role.USER)
                    .locked(false)
                    .build();

            assertThat(newUser.isLocked()).isFalse();
        }

        @Test
        @DisplayName("성공: 명시적으로 설정한 role이 반영된다")
        void setRole_explicitly() {
            User newUser = User.builder()
                    .name("test")
                    .email("test@mopl.io")
                    .password("password")
                    .role(Role.ADMIN)
                    .locked(false)
                    .build();

            assertThat(newUser.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("성공: id, name, email은 필수 필드다")
        void required_fields() {
            assertThat(user.getId()).isNotNull();
            assertThat(user.getName()).isNotBlank();
            assertThat(user.getEmail()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("체이닝 업데이트 테스트")
    class ChainedUpdates {

        @Test
        @DisplayName("성공: 여러 번 업데이트해도 각각 반영된다")
        void multipleUpdates() {
            user.updateProfile("buzz", "https://new-url.com");
            user.updatePassword("newPassword");
            user.updateRole(Role.ADMIN);
            user.updateLocked(true);

            assertThat(user.getName()).isEqualTo("buzz");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://new-url.com");
            assertThat(user.getPassword()).isEqualTo("newPassword");
            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
            assertThat(user.isLocked()).isTrue();
        }

        @Test
        @DisplayName("성공: 이름만 여러 번 업데이트하면 마지막 값이 적용된다")
        void profileUpdate_multipleNames() {
            user.updateProfile("buzz", null);
            user.updateProfile("woody2", null);
            user.updateProfile("final", null);

            assertThat(user.getName()).isEqualTo("final");
        }
    }
}
