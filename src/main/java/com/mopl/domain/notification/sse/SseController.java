package com.mopl.domain.notification.sse;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "sse-controller")
@RestController
@RequiredArgsConstructor
public class SseController {

  private final SseEmitterRegistry emitterRegistry;

  @GetMapping(value = "/api/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter connect(@RequestParam(required = false, name = "LastEventId") UUID lastEventId) {
    return emitterRegistry.connect(currentUserId());
  }

  private UUID currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof JwtClaims claims)) {
      throw new MoplException(ErrorCode.INVALID_TOKEN);
    }

    return claims.getUserId();
  }
}
