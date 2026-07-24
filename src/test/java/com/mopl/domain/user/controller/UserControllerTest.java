package com.mopl.domain.user.controller;

import com.mopl.domain.user.dto.*;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.GlobalExceptionHandler;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.response.CursorPageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.infra.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 테스트")
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private S3Service s3Service;

    private UUID userId;
    private UserDto userDto;
    private UUID authUserId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, s3Service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(JwtClaims.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return JwtClaims.builder().userId(authUserId).build();
                    }
                })
                .build();

        userId = UUID.randomUUID();
        authUserId = userId;
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
    @DisplayName("register: 사용자 등록")
    class RegisterTest {

        @Test
        @DisplayName("성공: 올바른 정보로 가입한다")
        void register_success() throws Exception {
            UserCreateRequest request = new UserCreateRequest("woody", "woody@mopl.io", "mopl1!23");
            given(userService.register(any())).willReturn(userDto);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(userId.toString()))
                    .andExpect(jsonPath("$.name").value("woody"));
        }

        @Test
        @DisplayName("성공: 다른 사용자도 등록할 수 있다")
        void register_success_anotherUser() throws Exception {
            UserCreateRequest request = new UserCreateRequest("buzz", "buzz@mopl.io", "mopl2!34");
            UserDto anotherUserDto = new UserDto(
                    UUID.randomUUID(),
                    Instant.now(),
                    "buzz@mopl.io",
                    "buzz",
                    null,
                    Role.USER,
                    false
            );
            given(userService.register(any())).willReturn(anotherUserDto);

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("buzz"));
        }

        @Test
        @DisplayName("실패: 중복된 이메일로 가입하려고 하면 409 Conflict 반환")
        void register_fail_duplicateEmail() throws Exception {
            UserCreateRequest request = new UserCreateRequest("woody", "woody@mopl.io", "mopl1!23");
            given(userService.register(any())).willThrow(new MoplException(ErrorCode.DUPLICATE_EMAIL));

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("실패: 중복된 이름으로 가입하려고 하면 409 Conflict 반환")
        void register_fail_duplicateName() throws Exception {
            UserCreateRequest request = new UserCreateRequest("woody", "new@mopl.io", "mopl1!23");
            given(userService.register(any())).willThrow(new MoplException(ErrorCode.DUPLICATE_NAME));

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("find: 사용자 단건 조회")
    class FindTest {

        @Test
        @DisplayName("성공: userId로 사용자를 조회한다")
        void find_success() throws Exception {
            given(userService.find(userId)).willReturn(userDto);

            mockMvc.perform(get("/api/users/" + userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()))
                    .andExpect(jsonPath("$.name").value("woody"));
        }

        @Test
        @DisplayName("성공: 다른 사용자도 조회할 수 있다")
        void find_success_anotherUser() throws Exception {
            UUID anotherId = UUID.randomUUID();
            UserDto anotherUserDto = new UserDto(
                    anotherId,
                    Instant.now(),
                    "buzz@mopl.io",
                    "buzz",
                    null,
                    Role.ADMIN,
                    false
            );
            given(userService.find(anotherId)).willReturn(anotherUserDto);

            mockMvc.perform(get("/api/users/" + anotherId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("buzz"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자를 조회하면 404 Not Found 반환")
        void find_fail_userNotFound() throws Exception {
            given(userService.find(userId)).willThrow(new MoplException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(get("/api/users/" + userId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("updateRole: 권한 수정")
    class UpdateRoleTest {

        @Test
        @DisplayName("성공: 사용자 권한을 ADMIN으로 변경한다")
        void updateRole_success_toAdmin() throws Exception {
            UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);
            given(userService.updateRole(userId, request)).willReturn(userDto);

            mockMvc.perform(patch("/api/users/" + userId + "/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()));
        }

        @Test
        @DisplayName("성공: 사용자 권한을 USER로 변경한다")
        void updateRole_success_toUser() throws Exception {
            UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.USER);
            given(userService.updateRole(userId, request)).willReturn(userDto);

            mockMvc.perform(patch("/api/users/" + userId + "/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자의 권한을 수정하려고 하면 404 Not Found 반환")
        void updateRole_fail_userNotFound() throws Exception {
            UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);
            given(userService.updateRole(userId, request)).willThrow(new MoplException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(patch("/api/users/" + userId + "/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("getUsers: 사용자 목록 조회")
    class GetUsersTest {

        @Test
        @DisplayName("성공: 사용자 목록을 조회한다")
        void getUsers_success() throws Exception {
            UserDto userDto1 = new UserDto(UUID.randomUUID(), Instant.now(), "user1@mopl.io", "user1", null, Role.USER, false);
            UserDto userDto2 = new UserDto(UUID.randomUUID(), Instant.now(), "user2@mopl.io", "user2", null, Role.USER, false);

            CursorPageResponse<UserDto> response = new CursorPageResponse<>(
                    List.of(userDto1, userDto2),
                    null, null, false, 2L,
                    "NAME", "ASCENDING"
            );
            given(userService.findAll(any())).willReturn(response);

            mockMvc.perform(get("/api/users")
                            .param("limit", "10")
                            .param("sortDirection", "ASCENDING")
                            .param("sortBy", "NAME"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        @DisplayName("성공: 필터 조건으로 사용자를 조회한다")
        void getUsers_success_withFilters() throws Exception {
            UserDto userDto1 = new UserDto(UUID.randomUUID(), Instant.now(), "admin@mopl.io", "admin", null, Role.ADMIN, false);

            CursorPageResponse<UserDto> response = new CursorPageResponse<>(
                    List.of(userDto1),
                    null, null, false, 1L,
                    "NAME", "ASCENDING"
            );
            given(userService.findAll(any())).willReturn(response);

            mockMvc.perform(get("/api/users")
                            .param("limit", "5")
                            .param("sortDirection", "ASCENDING")
                            .param("sortBy", "NAME")
                            .param("roleEqual", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.totalCount").value(1));
        }

        @Test
        @DisplayName("성공: 검색 결과가 없으면 빈 리스트를 반환한다")
        void getUsers_success_emptyResult() throws Exception {
            CursorPageResponse<UserDto> response = new CursorPageResponse<>(
                    List.of(),
                    null, null, false, 0L,
                    "NAME", "ASCENDING"
            );
            given(userService.findAll(any())).willReturn(response);

            mockMvc.perform(get("/api/users")
                    .param("limit", "10")
                    .param("sortDirection", "ASCENDING")
                    .param("sortBy", "NAME")
                    .param("emailLike", "nonexistent@mopl.io"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)))
                    .andExpect(jsonPath("$.totalCount").value(0));
        }
    }

    @Nested
    @DisplayName("updateLocked: 계정 잠금 상태 변경")
    class UpdateLockedTest {

        @Test
        @DisplayName("성공: 사용자 계정을 잠금 처리한다")
        void updateLocked_success_lock() throws Exception {
            UserLockUpdateRequest request = new UserLockUpdateRequest(true);
            UserDto lockedUserDto = new UserDto(
                    userId,
                    Instant.now(),
                    userDto.email(),
                    userDto.name(),
                    userDto.profileImageUrl(),
                    userDto.role(),
                    true
            );
            given(userService.updateLocked(userId, request)).willReturn(lockedUserDto);

            mockMvc.perform(patch("/api/users/" + userId + "/locked")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locked").value(true));
        }

        @Test
        @DisplayName("성공: 사용자 계정 잠금을 해제한다")
        void updateLocked_success_unlock() throws Exception {
            UserLockUpdateRequest request = new UserLockUpdateRequest(false);
            UserDto unlockedUserDto = new UserDto(
                    userId,
                    Instant.now(),
                    userDto.email(),
                    userDto.name(),
                    userDto.profileImageUrl(),
                    userDto.role(),
                    false
            );
            given(userService.updateLocked(userId, request)).willReturn(unlockedUserDto);

            mockMvc.perform(patch("/api/users/" + userId + "/locked")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locked").value(false));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자의 계정 상태를 변경하려고 하면 404 Not Found 반환")
        void updateLocked_fail_userNotFound() throws Exception {
            UserLockUpdateRequest request = new UserLockUpdateRequest(true);
            given(userService.updateLocked(userId, request)).willThrow(new MoplException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(patch("/api/users/" + userId + "/locked")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("updateProfile: 프로필 수정")
    class UpdateProfileTest {

        @Test
        @DisplayName("성공: 본인 프로필을 이미지 없이 수정한다")
        void updateProfile_success_noImage() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("buzz");
            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "request", MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsString(request).getBytes());
            UserDto updated = new UserDto(userId, Instant.now(), userDto.email(), "buzz", null, Role.USER, false);
            given(userService.updateProfile(eq(userId), eq(request), eq(null))).willReturn(updated);

            mockMvc.perform(multipart("/api/users/" + userId)
                            .file(requestPart)
                            .with(req -> { req.setMethod("PATCH"); return req; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("buzz"));

            verify(s3Service, never()).upload(any());
        }

        @Test
        @DisplayName("성공: 이미지를 포함하여 본인 프로필을 수정한다")
        void updateProfile_success_withImage() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("buzz");
            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "request", MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsString(request).getBytes());
            MockMultipartFile imagePart = new MockMultipartFile(
                    "image", "profile.png", MediaType.IMAGE_PNG_VALUE, "image-bytes".getBytes());
            UserDto updated = new UserDto(userId, Instant.now(), userDto.email(), "buzz", "https://s3.url/profile.png", Role.USER, false);

            given(s3Service.upload(any())).willReturn("https://s3.url/profile.png");
            given(s3Service.extractKey("https://s3.url/profile.png")).willReturn("profile.png");
            given(userService.updateProfile(eq(userId), eq(request), eq("profile.png"))).willReturn(updated);

            mockMvc.perform(multipart("/api/users/" + userId)
                            .file(requestPart)
                            .file(imagePart)
                            .with(req -> { req.setMethod("PATCH"); return req; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profileImageUrl").value("https://s3.url/profile.png"));
        }

        @Test
        @DisplayName("실패: 본인이 아닌 사용자의 프로필을 수정하려고 하면 403 Forbidden 반환")
        void updateProfile_fail_forbidden() throws Exception {
            UUID otherUserId = UUID.randomUUID();
            authUserId = UUID.randomUUID();
            UserUpdateRequest request = new UserUpdateRequest("buzz");
            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "request", MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsString(request).getBytes());

            mockMvc.perform(multipart("/api/users/" + otherUserId)
                            .file(requestPart)
                            .with(req -> { req.setMethod("PATCH"); return req; }))
                    .andExpect(status().isForbidden());

            verify(userService, never()).updateProfile(any(), any(), any());
        }

        @Test
        @DisplayName("실패: 이름이 비어있으면 400 Bad Request 반환")
        void updateProfile_fail_blankName() throws Exception {
            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "request", MediaType.APPLICATION_JSON_VALUE,
                    "{\"name\":\"\"}".getBytes());

            mockMvc.perform(multipart("/api/users/" + userId)
                            .file(requestPart)
                            .with(req -> { req.setMethod("PATCH"); return req; }))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 변경하려는 이름이 이미 존재하면 409 Conflict 반환")
        void updateProfile_fail_duplicateName() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("buzz");
            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "request", MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsString(request).getBytes());
            given(userService.updateProfile(eq(userId), eq(request), eq(null)))
                    .willThrow(new MoplException(ErrorCode.DUPLICATE_NAME));

            mockMvc.perform(multipart("/api/users/" + userId)
                            .file(requestPart)
                            .with(req -> { req.setMethod("PATCH"); return req; }))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("updatePassword: 비밀번호 변경")
    class UpdatePasswordTest {

        @Test
        @DisplayName("성공: 본인 비밀번호를 변경한다")
        void updatePassword_success() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("newPassword1!");
            given(userService.updatePassword(userId, request)).willReturn(userDto);

            mockMvc.perform(patch("/api/users/" + userId + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()));
        }

        @Test
        @DisplayName("실패: 본인이 아닌 사용자의 비밀번호를 변경하려고 하면 403 Forbidden 반환")
        void updatePassword_fail_forbidden() throws Exception {
            UUID otherUserId = UUID.randomUUID();
            authUserId = UUID.randomUUID();
            ChangePasswordRequest request = new ChangePasswordRequest("newPassword1!");

            mockMvc.perform(patch("/api/users/" + otherUserId + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(userService, never()).updatePassword(any(), any());
        }

        @Test
        @DisplayName("실패: 비밀번호가 비어있으면 400 Bad Request 반환")
        void updatePassword_fail_blank() throws Exception {
            mockMvc.perform(patch("/api/users/" + userId + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자면 404 Not Found 반환")
        void updatePassword_fail_userNotFound() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("newPassword1!");
            given(userService.updatePassword(userId, request)).willThrow(new MoplException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(patch("/api/users/" + userId + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("register: 유효성 검증")
    class RegisterValidationTest {

        @Test
        @DisplayName("실패: 이메일 형식이 올바르지 않으면 400 Bad Request 반환")
        void register_fail_invalidEmail() throws Exception {
            String invalidRequest = "{\"name\":\"woody\",\"email\":\"not-an-email\",\"password\":\"mopl1!\"}";

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).register(any());
        }

        @Test
        @DisplayName("실패: 이름이 비어있으면 400 Bad Request 반환")
        void register_fail_blankName() throws Exception {
            String invalidRequest = "{\"name\":\"\",\"email\":\"woody@mopl.io\",\"password\":\"mopl1!\"}";

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

}