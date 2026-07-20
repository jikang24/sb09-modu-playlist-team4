package com.mopl.domain.notification.sse;

import com.mopl.domain.notification.domain.Notification;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.global.dto.DirectMessageDto;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterRegistry implements SseNotificationSender {

  private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;
  private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
  private final NotificationRepository notificationRepository;

  @Scheduled(fixedRate = 30000) // 30초마다
  public void heartbeat() {
    emitters.forEach((userId, userEmitters) -> {
      for (SseEmitter emitter : userEmitters) {
        try {
          emitter.send(
              SseEmitter.event()
                  .name("heartbeat")
                  .data("ping")
          );
        } catch (IOException | IllegalStateException e) {
          log.trace("Failed to send heartbeat to userId: {}", userId);
          remove(userId, emitter);
        }
      }
    });
  }

  public SseEmitter connect(UUID userId, UUID lastEventId) {
    SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
    emitters.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);

    emitter.onCompletion(() -> remove(userId, emitter));
    emitter.onTimeout(() -> remove(userId, emitter));
    emitter.onError(error -> remove(userId, emitter));

    if (lastEventId != null && !replay(userId, emitter, lastEventId)) {
      return emitter;
    }

    try {
      emitter.send(SseEmitter.event()
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

  @Override
  public void sendDirectMessage(UUID receiverId, DirectMessageDto directMessage) {
    Set<SseEmitter> userEmitters = emitters.get(receiverId);
    if (userEmitters == null || userEmitters.isEmpty()) {
      return;
    }

    for (SseEmitter emitter : userEmitters) {
      try {
        emitter.send(SseEmitter.event()
            .id(directMessage.id().toString())
            .name("direct-messages")
            .data(directMessage));
      } catch (IOException | IllegalStateException e) {
        log.debug("SSE direct message send failed: receiverId={}, directMessageId={}",
            receiverId, directMessage.id(), e);
        remove(receiverId, emitter);
      }
    }
  }

  private boolean replay(UUID userId, SseEmitter emitter, UUID lastEventId) {
    for (Notification notification : notificationRepository.findByReceiverIdAfter(userId, lastEventId)) {
      try {
        emitter.send(SseEmitter.event()
            .id(notification.getId().toString())
            .name("notifications")
            .data(NotificationDto.from(notification)));
      } catch (IOException | IllegalStateException e) {
        log.debug("SSE replay failed: receiverId={}, notificationId={}", userId,
            notification.getId(), e);
        remove(userId, emitter);
        return false;
      }
    }

    return true;
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
