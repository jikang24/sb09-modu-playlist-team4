package com.mopl.domain.notification.sse;

import com.mopl.domain.notification.domain.Notification;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.global.dto.DirectMessageDto;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterRegistry implements SseNotificationSender {

  private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;
  private static final int MAX_REPLAY_COUNT = 200;

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
    List<Notification> notifications =
        notificationRepository.findByReceiverIdAfter(userId, lastEventId, MAX_REPLAY_COUNT + 1);
    boolean truncated = notifications.size() > MAX_REPLAY_COUNT;
    List<Notification> toSend = truncated ? notifications.subList(0, MAX_REPLAY_COUNT) : notifications;

    for (Notification notification : toSend) {
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

    if (truncated) {
      try {
        emitter.send(SseEmitter.event()
            .name("replay-truncated")
            .data("미확인 알림이 %d건을 초과합니다. 새로고침해주세요.".formatted(MAX_REPLAY_COUNT)));
      } catch (IOException | IllegalStateException e) {
        log.debug("SSE replay-truncated notice failed: receiverId={}", userId, e);
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