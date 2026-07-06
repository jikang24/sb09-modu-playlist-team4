package com.mopl.domain.playlist.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.playlist.application.dto.PlaylistCreateRequest;
import com.mopl.domain.playlist.application.dto.PlaylistDto;
import com.mopl.domain.playlist.application.dto.PlaylistUpdateRequest;
import com.mopl.domain.playlist.application.port.in.PlaylistUseCase;
import com.mopl.global.auth.UserAuthPort;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.jwt.JwtAuthenticationFilter;
import com.mopl.global.jwt.JwtProperties;
import com.mopl.global.security.csrf.CsrfCookieFilter;
import com.mopl.global.security.handler.MoplAccessDeniedHandler;
import com.mopl.global.security.handler.MoplAuthenticationEntryPoint;
import com.mopl.global.security.handler.MoplLoginFailureHandler;
import com.mopl.global.security.handler.MoplLoginSuccessHandler;
import com.mopl.global.security.handler.MoplLogoutHandler;
import com.mopl.global.security.handler.MoplLogoutSuccessHandler;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.mopl.global.config.SecurityConfig;
import com.mopl.global.security.MoplAuthenticationProvider;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@WebMvcTest(controllers = PlaylistController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class PlaylistControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PlaylistUseCase playlistUseCase;
    @MockBean private MoplAuthenticationProvider moplAuthenticationProvider;

    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private CsrfCookieFilter csrfCookieFilter;
    @MockBean private MoplAuthenticationEntryPoint authenticationEntryPoint;
    @MockBean private MoplAccessDeniedHandler accessDeniedHandler;
    @MockBean private PasswordResetTokenPort passwordResetTokenPort;
    @MockBean private MoplLoginSuccessHandler loginSuccessHandler;
    @MockBean private MoplLoginFailureHandler loginFailureHandler;
    @MockBean private MoplLogoutHandler logoutHandler;
    @MockBean private MoplLogoutSuccessHandler logoutSuccessHandler;
    @MockBean private UserAuthPort userAuthPort;
    @MockBean private JwtProperties jwtProperties;

    private UUID userId;
    private UUID playlistId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        playlistId = UUID.randomUUID();
    }

    @Test
    @WithMockUser
    @DisplayName("플레이리스트 생성 API 성공")
    void createPlaylist_success() throws Exception {
        PlaylistCreateRequest request = new PlaylistCreateRequest("Title", "Description");
        PlaylistDto response = new PlaylistDto(playlistId, new UserSummary(userId, "Owner", null), "Title", "Description", Instant.now(), 0, false, List.of());
        
        given(playlistUseCase.create(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/playlists")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("플레이리스트 조회 API 성공")
    void getPlaylist_success() throws Exception {
        PlaylistDto response = new PlaylistDto(playlistId, new UserSummary(userId, "Owner", null), "Title", "Description", Instant.now(), 0, false, List.of());
        given(playlistUseCase.getById(eq(playlistId), any())).willReturn(response);

        mockMvc.perform(get("/api/playlists/{playlistId}", playlistId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("플레이리스트 수정 API 성공")
    void updatePlaylist_success() throws Exception {
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("New Title", "New Description");
        PlaylistDto response = new PlaylistDto(playlistId, new UserSummary(userId, "Owner", null), "New Title", "New Description", Instant.now(), 0, false, List.of());
        given(playlistUseCase.update(eq(playlistId), any(), any())).willReturn(response);

        mockMvc.perform(patch("/api/playlists/{playlistId}", playlistId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("플레이리스트 삭제 API 성공")
    void deletePlaylist_success() throws Exception {
        mockMvc.perform(delete("/api/playlists/{playlistId}", playlistId)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("플레이리스트 구독 API 성공")
    void subscribe_success() throws Exception {
        mockMvc.perform(post("/api/playlists/{playlistId}/subscription", playlistId)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("콘텐츠 추가 API 성공")
    void addContent_success() throws Exception {
        UUID contentId = UUID.randomUUID();
        mockMvc.perform(post("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
