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
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.mopl.global.security.MoplAuthenticationProvider;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@WebMvcTest(controllers = PlaylistController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
class PlaylistControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private PlaylistUseCase playlistUseCase;
    @MockitoBean private MoplAuthenticationProvider moplAuthenticationProvider;

    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private CsrfCookieFilter csrfCookieFilter;
    @MockitoBean private MoplAuthenticationEntryPoint authenticationEntryPoint;
    @MockitoBean private MoplAccessDeniedHandler accessDeniedHandler;
    @MockitoBean private PasswordResetTokenPort passwordResetTokenPort;
    @MockitoBean private MoplLoginSuccessHandler loginSuccessHandler;
    @MockitoBean private MoplLoginFailureHandler loginFailureHandler;
    @MockitoBean private MoplLogoutHandler logoutHandler;
    @MockitoBean private MoplLogoutSuccessHandler logoutSuccessHandler;
    @MockitoBean private UserAuthPort userAuthPort;
    @MockitoBean private JwtProperties jwtProperties;

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
