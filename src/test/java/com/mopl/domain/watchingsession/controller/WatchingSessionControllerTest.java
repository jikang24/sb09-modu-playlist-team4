package com.mopl.domain.watchingsession.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.mopl.domain.watchingsession.service.WatchingSessionService;
import com.mopl.global.dto.ContentSummary;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.GlobalExceptionHandler;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.response.CursorPageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchingSessionController 테스트")
class WatchingSessionControllerTest {

  private MockMvc mockMvc;

  @Mock
  private WatchingSessionService watchingSessionService;

  private UUID myUserId;

  @BeforeEach
  void setUp() {
    myUserId = UUID.randomUUID();
    JwtClaims claims = JwtClaims.builder().userId(myUserId).build();

    // 컨트롤러가 @AuthenticationPrincipal JwtClaims로 인증 사용자를 받으므로,
    // Security 전체를 띄우는 대신 이 파라미터만 고정값으로 채워주는 리졸버를 등록한다.
    mockMvc = MockMvcBuilders.standaloneSetup(new WatchingSessionController(watchingSessionService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(JwtClaims.class);
          }

          @Override
          public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return claims;
          }
        })
        .build();
  }

  private WatchingSessionDto makeDto(UUID watcherId, UUID contentId) {
    UserSummary watcher = new UserSummary(watcherId, "닉네임", "https://profile.jpg");
    ContentSummary content = new ContentSummary(
        contentId, ContentType.MOVIE, "제목", "설명", "https://thumb.jpg",
        List.of("액션"), BigDecimal.valueOf(4.5), 10);
    return new WatchingSessionDto(UUID.randomUUID(), Instant.parse("2026-01-01T00:00:00Z"), watcher, content);
  }

  @Nested
  @DisplayName("GET /api/users/{watcherId}/watching-sessions")
  class GetByWatcher {

    @Test
    @DisplayName("성공: 현재 시청 세션을 조회한다")
    void success() throws Exception {
      UUID watcherId = UUID.randomUUID();
      UUID contentId = UUID.randomUUID();
      WatchingSessionDto dto = makeDto(watcherId, contentId);

      given(watchingSessionService.getByWatcherId(watcherId)).willReturn(dto);

      mockMvc.perform(get("/api/users/" + watcherId + "/watching-sessions"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.watcher.userId").value(watcherId.toString()))
          .andExpect(jsonPath("$.content.id").value(contentId.toString()));
    }

    @Test
    @DisplayName("성공: 시청 중인 세션이 없으면 빈 응답(null)을 반환한다")
    void success_noSession() throws Exception {
      UUID watcherId = UUID.randomUUID();
      given(watchingSessionService.getByWatcherId(watcherId)).willReturn(null);

      mockMvc.perform(get("/api/users/" + watcherId + "/watching-sessions"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").doesNotExist());
    }
  }

  @Nested
  @DisplayName("DELETE /api/users/{watcherId}/watching-sessions")
  class Leave {

    @Test
    @DisplayName("성공: 본인 세션이면 퇴장 처리 후 204 No Content")
    void success() throws Exception {
      mockMvc.perform(delete("/api/users/" + myUserId + "/watching-sessions"))
          .andExpect(status().isNoContent());

      verify(watchingSessionService).leave(myUserId);
    }

    @Test
    @DisplayName("실패: 본인이 아닌 다른 사용자의 세션을 종료하려 하면 403 Forbidden")
    void fail_notSelf() throws Exception {
      UUID otherWatcherId = UUID.randomUUID();

      mockMvc.perform(delete("/api/users/" + otherWatcherId + "/watching-sessions"))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("실패: 보고 있는 세션이 없으면 404 Not Found")
    void fail_notFound() throws Exception {
      doThrow(new MoplException(ErrorCode.WATCHING_SESSION_NOT_FOUND))
          .when(watchingSessionService).leave(myUserId);

      mockMvc.perform(delete("/api/users/" + myUserId + "/watching-sessions"))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("GET /api/contents/{contentId}/watching-sessions")
  class GetByContent {

    @Test
    @DisplayName("성공: 콘텐츠별 시청 세션 목록을 조회한다")
    void success() throws Exception {
      UUID contentId = UUID.randomUUID();
      WatchingSessionDto dto = makeDto(UUID.randomUUID(), contentId);
      CursorPageResponse<WatchingSessionDto> response = new CursorPageResponse<>(
          List.of(dto), null, null, false, 1L, "createdAt", "DESCENDING");

      given(watchingSessionService.getByContentId(any())).willReturn(response);

      mockMvc.perform(get("/api/contents/" + contentId + "/watching-sessions")
              .param("limit", "10")
              .param("sortBy", "createdAt")
              .param("sortDirection", "DESCENDING"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(1)))
          .andExpect(jsonPath("$.hasNext").value(false))
          .andExpect(jsonPath("$.totalCount").value(1));
    }
  }

  @Nested
  @DisplayName("POST /api/contents/{contentId}/watching-sessions")
  class Enter {

    @Test
    @DisplayName("성공: 요청자 본인이 watcher가 되어 시청 세션에 입장한다")
    void success() throws Exception {
      UUID contentId = UUID.randomUUID();
      WatchingSessionDto dto = makeDto(myUserId, contentId);

      given(watchingSessionService.enter(myUserId, contentId)).willReturn(dto);

      mockMvc.perform(post("/api/contents/" + contentId + "/watching-sessions"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.watcher.userId").value(myUserId.toString()));

      verify(watchingSessionService).enter(eq(myUserId), eq(contentId));
    }
  }
}
