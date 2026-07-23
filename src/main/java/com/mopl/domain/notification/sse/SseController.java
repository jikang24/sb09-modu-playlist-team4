package com.mopl.domain.notification.sse;

import com.mopl.domain.notification.support.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "알림")
@RestController
@RequiredArgsConstructor
public class SseController {

  private final SseEmitterRegistry emitterRegistry;
  private final CurrentUserProvider currentUserProvider;

  @Operation(summary = "실시간 알림 구독 (SSE)", description = "Last-Event-ID로 재연결 시 끊긴 시점 이후의 알림을 이어받을 수 있습니다.")
  @GetMapping(value = "/api/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter connect(
      @RequestHeader(value = "Last-Event-ID", required = false) UUID lastEventIdHeader,
      @RequestParam(required = false, name = "LastEventId") UUID lastEventIdQuery,
      HttpServletResponse response
  ) {
    response.setHeader("X-Accel-Buffering", "no");
    UUID lastEventId = lastEventIdQuery != null ? lastEventIdQuery : lastEventIdHeader;
    return emitterRegistry.connect(currentUserProvider.getCurrentUserId(), lastEventId);
  }
}
