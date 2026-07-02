package com.mopl.domain.notification.sse;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.GlobalExceptionHandler;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

  private UUID userId;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new SseController(emitterRegistry))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    userId = UUID.randomUUID();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate() {
    JwtClaims claims = JwtClaims.builder()
        .userId(userId)
        .email("user@mopl.io")
        .role("USER")
        .tokenId("token-id")
        .build();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(claims, null, List.of())
    );
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

    mockMvc.perform(get("/api/sse").param("LastEventId", lastEventId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("성공: Last-Event-ID 헤더를 사용한다")
  void connect_withLastEventIdHeader() throws Exception {
    authenticate();
    UUID lastEventId = UUID.randomUUID();
    SseEmitter emitter = new SseEmitter();
    given(emitterRegistry.connect(userId, lastEventId)).willReturn(emitter);

    mockMvc.perform(get("/api/sse").header("Last-Event-ID", lastEventId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("실패: 인증 정보가 없으면 401 Unauthorized")
  void connect_noAuthentication() throws Exception {
    mockMvc.perform(get("/api/sse"))
        .andExpect(status().isUnauthorized());
  }
}
