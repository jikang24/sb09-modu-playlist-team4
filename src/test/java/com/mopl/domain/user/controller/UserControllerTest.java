package com.mopl.domain.user.controller;

import com.mopl.domain.user.dto.*;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import com.mopl.infra.s3.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

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

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, s3Service)).build();

        userId = UUID.randomUUID();
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
            UserCreateRequest request = new UserCreateRequest("woody", "woody@mopl.io", "mopl1!");
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
            UserCreateRequest request = new UserCreateRequest("buzz", "buzz@mopl.io", "mopl2!");
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
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("buzz"));
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
    }
}
