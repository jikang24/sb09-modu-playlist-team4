package com.mopl.domain.notification.sse;

import com.mopl.domain.notification.dto.NotificationDto;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class SseEmitterRegistry implements SseNotificationSender {

  private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;
  private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

  public SseEmitter connect(UUID userId) {
    SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
    emitters.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);

    emitter.onCompletion(() -> remove(userId, emitter));
    emitter.onTimeout(() -> remove(userId, emitter));
    emitter.onError(error -> remove(userId, emitter));

    try {
      emitter.send(SseEmitter.event()
          .id(UUID.randomUUID().toString())
          .name("connect")
          .data("connected"));
    } catch (IOException e) {
      remove(userId, emitter);
    }

    return emitter;
  }

  @Override
  public void send(UUID receiverId, NotificationDto notification) {
    Set<SseEmitter> userEmitters = emitters.get(receiverId);
    if (userEmitters == null || userEmitters.isEmpty()) {
      return;
    }

    for (SseEmitter emitter : userEmitters) {
      try {
        emitter.send(SseEmitter.event()
            .id(notification.id().toString())
            .name("notifications")
            .data(notification));
      } catch (IOException | IllegalStateException e) {
        log.debug("SSE notification send failed: receiverId={}, notificationId={}",
            receiverId, notification.id(), e);
        remove(receiverId, emitter);
      }
    }
  }

  private void remove(UUID userId, SseEmitter emitter) {
    Set<SseEmitter> userEmitters = emitters.get(userId);
    if (userEmitters == null) {
      return;
    }

    userEmitters.remove(emitter);
    if (userEmitters.isEmpty()) {
      emitters.remove(userId);
    }
  }
}
