package com.mopl.domain.notification.service;

import com.mopl.domain.notification.domain.Notification;
import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.dto.NotificationSearchRequest;
import com.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import com.mopl.infra.redis.RedisNotificationPublisher;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mopl.domain.notification.support.CurrentUserProvider;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final RedisNotificationPublisher redisNotificationPublisher;
  private final CurrentUserProvider currentUserProvider;

  @Override
  @Transactional
  public NotificationDto send(UUID eventId, UUID receiverId, NotificationType type,
      String title, String content) {
    return notificationRepository.findByEventId(eventId)
        .map(existing -> {
          log.debug("중복 알림 이벤트 - DB 스킵, SSE fanout 재시도: eventId={}", eventId);
          NotificationDto dto = NotificationDto.from(existing);
          redisNotificationPublisher.publish(dto);
          return dto;
        })
        .orElseGet(() -> persistAndFanout(eventId, receiverId, type, title, content));
  }

  private NotificationDto persistAndFanout(UUID eventId, UUID receiverId, NotificationType type,
      String title, String content) {
    try {
      Notification notification = Notification.builder()
          .eventId(eventId)
          .receiverId(receiverId)
          .type(type)
          .title(title)
          .content(content)
          .build();

      NotificationDto dto = NotificationDto.from(notificationRepository.save(notification));
      redisNotificationPublisher.publish(dto);
      return dto;
    } catch (DataIntegrityViolationException e) {
      // 동시 재전달 등으로 유니크 제약 충돌 시 기존 알림 반환
      return notificationRepository.findByEventId(eventId)
          .map(NotificationDto::from)
          .orElseThrow(() -> e);
    }
  }

  @Override
  public CursorPageResponse<NotificationDto> findMyNotifications(NotificationSearchRequest request) {
    validateSearchRequest(request);

    UUID receiverId = currentUserProvider.getCurrentUserId();
    List<Notification> notifications =
        notificationRepository.findUnreadByReceiverWithCursor(receiverId, request);

    boolean hasNext = notifications.size() > request.limit();
    if (hasNext) {
      notifications = notifications.subList(0, request.limit());
    }

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !notifications.isEmpty()) {
      Notification last = notifications.get(notifications.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    return new CursorPageResponse<>(
        notifications.stream().map(NotificationDto::from).toList(),
        nextCursor,
        nextIdAfter,
        hasNext,
        notificationRepository.countByReceiverIdAndIsReadFalse(receiverId),
        request.sortBy().name(),
        request.sortDirection().name()
    );
  }

  @Override
  @Transactional
  public void read(UUID notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new MoplException(ErrorCode.NOTIFICATION_NOT_FOUND));

    if (!notification.getReceiverId().equals(currentUserProvider.getCurrentUserId())) {
      throw new MoplException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
    }

    notification.read();
  }

  private void validateSearchRequest(NotificationSearchRequest request) {
    if (request.limit() < 1) {
      throw new MoplException(ErrorCode.INVALID_NOTIFICATION_SEARCH_REQUEST);
    }

    boolean hasCursor = request.cursor() != null && !request.cursor().isBlank();
    boolean hasIdAfter = request.idAfter() != null;
    if (hasCursor != hasIdAfter) {
      throw new MoplException(ErrorCode.INVALID_NOTIFICATION_CURSOR);
    }

    if (hasCursor) {
      try {
        Instant.parse(request.cursor());
      } catch (DateTimeParseException e) {
        throw new MoplException(ErrorCode.INVALID_NOTIFICATION_CURSOR);
      }
    }
  }
}
