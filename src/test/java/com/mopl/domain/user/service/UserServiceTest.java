package com.mopl.domain.user.service;

import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.*;
import com.mopl.domain.user.event.UserLockedEvent;
import com.mopl.domain.user.event.UserRoleChangedEvent;
import com.mopl.domain.user.mapper.UserMapper;
import com.mopl.domain.user.repository.SocialAccountRepository;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import com.mopl.infra.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SocialAccountRepository socialAccountRepository;
    @Mock
    private S3Service s3Service;

    private UserServiceImpl userService;

    private UUID userId;
    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder, eventPublisher, socialAccountRepository, s3Service);

        userId = UUID.randomUUID();
        user = User.builder()
                .name("woody")
                .email("woody@mopl.io")
                .password("encodedPassword")
                .role(Role.USER)
                .locked(false)
                .build();

        userDto = new UserDto(
                userId,
                Instant.now(),
                "woody@mopl.io",
                "woody",
                null,
                Role.USER,
                false
        );
    }

    @Nested
    @DisplayName("register: 회원 가입")
    class Register {

        @Test
        @DisplayName("성공: 이메일/이름 중복이 없으면 가입된다")
        void register_success() {
            UserCreateRequest request = new UserCreateRequest("woody", "woody@mopl.io", "mopl1!");

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(userRepository.existsByName(request.name())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(user);
            given(userMapper.toDto(user)).willReturn(userDto);

            UserDto result = userService.register(request);

            assertThat(result).isEqualTo(userDto);
            verify(passwordEncoder).encode("mopl1!");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("실패: 이메일이 중복되면 DUPLICATE_EMAIL 예외가 발생한다")
        void register_fail_duplicateEmail() {
            UserCreateRequest request = new UserCreateRequest("woody", "woody@mopl.io", "mopl1!");
            given(userRepository.existsByEmail(request.email())).willReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(MoplException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 이름이 중복되면 DUPLICATE_NAME 예외가 발생한다")
        void register_fail_duplicateName() {
            UserCreateRequest request = new UserCreateRequest("woody", "woody@mopl.io", "mopl1!");
            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(userRepository.existsByName(request.name())).willReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(MoplException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("find: 사용자 단건 조회")
    class Find {

        @Test
        @DisplayName("성공: 존재하는 userId로 조회한다")
        void find_success() {
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userMapper.toDto(user)).willReturn(userDto);

            UserDto result = userService.find(userId);

            assertThat(result).isEqualTo(userDto);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 userId면 USER_NOT_FOUND 예외가 발생한다")
        void find_fail_notFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.find(userId))
                    .isInstanceOf(MoplException.class);
        }
    }

    @Nested
    @DisplayName("updateProfile: 프로필 수정")
    class UpdateProfile {

        @Test
        @DisplayName("성공: 이름 변경 없이 이미지만 변경한다")
        void updateProfile_success_sameName() {
            UserUpdateRequest request = new UserUpdateRequest("woody");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userMapper.toDto(user)).willReturn(userDto);

            UserDto result = userService.updateProfile(userId, request, "https://image.url/new.png");

            assertThat(result).isEqualTo(userDto);
            verify(userRepository, never()).existsByName(anyString());
        }

        @Test
        @DisplayName("성공: 새 이름으로 변경한다")
        void updateProfile_success_changeName() {
            UserUpdateRequest request = new UserUpdateRequest("buzz");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.existsByName("buzz")).willReturn(false);
            given(userMapper.toDto(user)).willReturn(userDto);

            UserDto result = userService.updateProfile(userId, request, "https://image.url/new.png");

            assertThat(result).isEqualTo(userDto);
            verify(userRepository).existsByName("buzz");
        }

        @Test
        @DisplayName("실패: 변경하려는 이름이 이미 존재하면 DUPLICATE_NAME 예외가 발생한다")
        void updateProfile_fail_duplicateName() {
            UserUpdateRequest request = new UserUpdateRequest("buzz");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.existsByName("buzz")).willReturn(true);

            assertThatThrownBy(() -> userService.updateProfile(userId, request, "url"))
                    .isInstanceOf(MoplException.class);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자면 USER_NOT_FOUND 예외가 발생한다")
        void updateProfile_fail_notFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());
            UserUpdateRequest request = new UserUpdateRequest("buzz");

            assertThatThrownBy(() -> userService.updateProfile(userId, request, "url"))
                    .isInstanceOf(MoplException.class);
        }
    }

    @Nested
    @DisplayName("updateRole: 권한 변경")
    class UpdateRole {

        @Test
        @DisplayName("성공: 권한을 변경하고 UserRoleChangedEvent를 발행한다")
        void updateRole_success() {
            UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userMapper.toDto(user)).willReturn(userDto);

            userService.updateRole(userId, request);

            ArgumentCaptor<UserRoleChangedEvent> captor = ArgumentCaptor.forClass(UserRoleChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().oldRole()).isEqualTo(Role.USER);
            assertThat(captor.getValue().newRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자면 USER_NOT_FOUND 예외가 발생하고 이벤트가 발행되지 않는다")
        void updateRole_fail_notFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());
            UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

            assertThatThrownBy(() -> userService.updateRole(userId, request))
                    .isInstanceOf(MoplException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("성공: 기존 권한과 동일한 권한이면 이벤트를 발행하지 않는다")
        void updateRole_success_sameRole_doesNotPublishEvent() {
            UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.USER);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userMapper.toDto(user)).willReturn(userDto);

            userService.updateRole(userId, request);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("updatePassword: 비밀번호 변경")
    class UpdatePassword {

        @Test
        @DisplayName("성공: 비밀번호를 인코딩하여 변경한다")
        void updatePassword_success() {
            ChangePasswordRequest request = new ChangePasswordRequest("newPassword1!");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.encode("newPassword1!")).willReturn("encodedNewPassword");
            given(userMapper.toDto(user)).willReturn(userDto);

            UserDto result = userService.updatePassword(userId, request);

            assertThat(result).isEqualTo(userDto);
            verify(passwordEncoder).encode("newPassword1!");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자면 USER_NOT_FOUND 예외가 발생한다")
        void updatePassword_fail_notFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());
            ChangePasswordRequest request = new ChangePasswordRequest("newPassword1!");

            assertThatThrownBy(() -> userService.updatePassword(userId, request))
                    .isInstanceOf(MoplException.class);
        }
    }

    @Nested
    @DisplayName("updateLocked: 계정 잠금 상태 변경")
    class UpdateLocked {

        @Test
        @DisplayName("성공: locked=true로 변경하면 UserLockedEvent가 발행된다")
        void updateLocked_success_locked() {
            UserLockUpdateRequest request = new UserLockUpdateRequest(true);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userMapper.toDto(user)).willReturn(userDto);

            userService.updateLocked(userId, request);

            verify(eventPublisher, times(1)).publishEvent(any(UserLockedEvent.class));
        }

        @Test
        @DisplayName("성공: locked=false로 변경하면 이벤트가 발행되지 않는다")
        void updateLocked_success_unlocked() {
            UserLockUpdateRequest request = new UserLockUpdateRequest(false);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userMapper.toDto(user)).willReturn(userDto);

            userService.updateLocked(userId, request);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자면 USER_NOT_FOUND 예외가 발생한다")
        void updateLocked_fail_notFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());
            UserLockUpdateRequest request = new UserLockUpdateRequest(true);

            assertThatThrownBy(() -> userService.updateLocked(userId, request))
                    .isInstanceOf(MoplException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("findAll: 커서 기반 목록 조회")
    class FindAll {

        private UserSearchRequest baseRequest(int limit) {
            return new UserSearchRequest(
                    null, null, null, null, null, null,
                    limit, SortDirection.ASCENDING, UserSortBy.NAME
            );
        }

        @Test
        @DisplayName("성공: 다음 페이지가 있으면 hasNext=true이고 limit 개수만 반환한다")
        void findAll_hasNext_true() {
            UserSearchRequest request = baseRequest(2);
            User u1 = userOf("a");
            User u2 = userOf("b");
            User u3 = userOf("c");
            given(userRepository.findAllWithCursor(request)).willReturn(List.of(u1, u2, u3));
            given(userRepository.countAll(request)).willReturn(10L);
            given(userMapper.toDto(any(User.class))).willReturn(userDto);

            CursorPageResponse<UserDto> result = userService.findAll(request);

            assertThat(result.hasNext()).isTrue();
            assertThat(result.data()).hasSize(2);
            assertThat(result.totalCount()).isEqualTo(10L);
            assertThat(result.sortBy()).isEqualTo("NAME");
            assertThat(result.sortDirection()).isEqualTo("ASCENDING");
        }

        @Test
        @DisplayName("성공: 다음 페이지가 없으면 hasNext=false이고 nextCursor는 null이다")
        void findAll_hasNext_false() {
            UserSearchRequest request = baseRequest(5);
            User u1 = userOf("a");
            given(userRepository.findAllWithCursor(request)).willReturn(List.of(u1));
            given(userRepository.countAll(request)).willReturn(1L);
            given(userMapper.toDto(any(User.class))).willReturn(userDto);

            CursorPageResponse<UserDto> result = userService.findAll(request);

            assertThat(result.hasNext()).isFalse();
            assertThat(result.data()).hasSize(1);
            assertThat(result.nextCursor()).isNull();
            assertThat(result.nextIdAfter()).isNull();
        }

        @ParameterizedTest(name = "sortBy={0} 기준으로 nextCursor가 채워진다")
        @EnumSource(UserSortBy.class)
        @DisplayName("성공: sortBy 종류별로 nextCursor 분기가 정상 동작한다")
        void findAll_nextCursor_bySortBy(UserSortBy sortBy) {
            UserSearchRequest request = new UserSearchRequest(
                    null, null, null, null, null, null,
                    1, SortDirection.ASCENDING, sortBy
            );
            User u1 = userOf("a");
            User u2 = userOf("b");
            given(userRepository.findAllWithCursor(request)).willReturn(List.of(u1, u2));
            given(userRepository.countAll(request)).willReturn(2L);
            given(userMapper.toDto(any(User.class))).willReturn(userDto);

            CursorPageResponse<UserDto> result = userService.findAll(request);

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isNotNull();
            assertThat(result.nextIdAfter()).isNotNull();
        }

        @Test
        @DisplayName("성공: 빈 목록을 반환한다")
        void findAll_empty() {
            UserSearchRequest request = baseRequest(10);
            given(userRepository.findAllWithCursor(request)).willReturn(List.of());
            given(userRepository.countAll(request)).willReturn(0L);

            CursorPageResponse<UserDto> result = userService.findAll(request);

            assertThat(result.hasNext()).isFalse();
            assertThat(result.data()).isEmpty();
            assertThat(result.totalCount()).isZero();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.nextIdAfter()).isNull();
        }

        @Test
        @DisplayName("성공: limit과 정확히 같은 수의 결과면 hasNext=false이다")
        void findAll_exactLimit() {
            UserSearchRequest request = baseRequest(2);
            User u1 = userOf("a");
            User u2 = userOf("b");
            given(userRepository.findAllWithCursor(request)).willReturn(List.of(u1, u2));
            given(userRepository.countAll(request)).willReturn(2L);
            given(userMapper.toDto(any(User.class))).willReturn(userDto);

            CursorPageResponse<UserDto> result = userService.findAll(request);

            assertThat(result.hasNext()).isFalse();
            assertThat(result.data()).hasSize(2);
        }

        @Test
        @DisplayName("성공: DESCENDING 정렬도 정상 동작한다")
        void findAll_descending() {
            UserSearchRequest request = new UserSearchRequest(
                    null, null, null, null, null, null,
                    10, SortDirection.DESCENDING, UserSortBy.NAME
            );
            User u1 = userOf("a");
            given(userRepository.findAllWithCursor(request)).willReturn(List.of(u1));
            given(userRepository.countAll(request)).willReturn(1L);
            given(userMapper.toDto(any(User.class))).willReturn(userDto);

            CursorPageResponse<UserDto> result = userService.findAll(request);

            assertThat(result.sortDirection()).isEqualTo("DESCENDING");
            assertThat(result.hasNext()).isFalse();
        }

        private User userOf(String name) {
            return User.builder()
                    .id(UUID.randomUUID())
                    .name(name)
                    .email(name + "@mopl.io")
                    .password("encoded")
                    .role(Role.USER)
                    .locked(false)
                    .createdAt(Instant.now())
                    .build();
        }
    }

    @Nested
    @DisplayName("updateProfile: 엣지 케이스")
    class UpdateProfileEdgeCases {

        @Test
        @DisplayName("성공: 이름을 같은 값으로 변경하면 중복 체크를 하지 않는다")
        void updateProfile_sameNameNoCheck() {
            UserUpdateRequest request = new UserUpdateRequest("woody");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userMapper.toDto(user)).willReturn(userDto);

            UserDto result = userService.updateProfile(userId, request, null);

            assertThat(result).isEqualTo(userDto);
            verify(userRepository, never()).existsByName("woody");
        }

        @Test
        @DisplayName("성공: 이미지 URL을 변경해도 이름 중복 체크를 하지 않는다")
        void updateProfile_imageOnly() {
            UserUpdateRequest request = new UserUpdateRequest("woody");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userMapper.toDto(user)).willReturn(userDto);

            userService.updateProfile(userId, request, "https://new-image.url");

            verify(userRepository, never()).existsByName(anyString());
        }
    }

    @Nested
    @DisplayName("updatePassword: 엣지 케이스")
    class UpdatePasswordEdgeCases {

        @Test
        @DisplayName("성공: 비밀번호를 여러 번 변경할 수 있다")
        void updatePassword_multiple() {
            ChangePasswordRequest request1 = new ChangePasswordRequest("pass1");
            ChangePasswordRequest request2 = new ChangePasswordRequest("pass2");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.encode("pass1")).willReturn("encoded1");
            given(passwordEncoder.encode("pass2")).willReturn("encoded2");
            given(userMapper.toDto(user)).willReturn(userDto);

            userService.updatePassword(userId, request1);
            userService.updatePassword(userId, request2);

            verify(passwordEncoder, times(2)).encode(anyString());
        }
    }
}
