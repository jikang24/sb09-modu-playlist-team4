package com.mopl.domain.notification.sse;

import com.mopl.domain.notification.support.CurrentUserProvider;
import com.mopl.global.exception.GlobalExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("SseController 테스트")
class SseControllerTest {

  private MockMvc mockMvc;

  @Mock
  private SseEmitterRegistry emitterRegistry;

  @Mock
  private CurrentUserProvider currentUserProvider;

  private UUID userId;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new SseController(emitterRegistry, currentUserProvider))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    userId = UUID.randomUUID();
  }

  private void authenticate() {
    given(currentUserProvider.getCurrentUserId()).willReturn(userId);
  }

  @Test
  @DisplayName("성공: SSE 연결 시 emitter를 반환한다")
  void connect_success() throws Exception {
    authenticate();

    SseEmitter emitter = new SseEmitter();
    given(emitterRegistry.connect(eq(userId), isNull())).willReturn(emitter);

    mockMvc.perform(get("/api/sse"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("성공: LastEventId 쿼리 파라미터를 우선 사용한다")
  void connect_withLastEventIdQuery() throws Exception {
    authenticate();

    UUID lastEventId = UUID.randomUUID();
    SseEmitter emitter = new SseEmitter();
    given(emitterRegistry.connect(userId, lastEventId)).willReturn(emitter);

    mockMvc.perform(get("/api/sse")
            .param("LastEventId", lastEventId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("성공: Last-Event-ID 헤더를 사용한다")
  void connect_withLastEventIdHeader() throws Exception {
    authenticate();

    UUID lastEventId = UUID.randomUUID();
    SseEmitter emitter = new SseEmitter();
    given(emitterRegistry.connect(userId, lastEventId)).willReturn(emitter);

    mockMvc.perform(get("/api/sse")
            .header("Last-Event-ID", lastEventId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("실패: 인증 정보가 없으면 401 Unauthorized")
  void connect_noAuthentication() throws Exception {
    given(currentUserProvider.getCurrentUserId())
        .willThrow(new com.mopl.global.exception.MoplException(
            com.mopl.global.exception.ErrorCode.INVALID_TOKEN));

    mockMvc.perform(get("/api/sse"))
        .andExpect(status().isUnauthorized());
  }
}