package com.mopl.domain.notification.repository;

import com.mopl.domain.notification.domain.Notification;
import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.domain.notification.dto.NotificationSearchRequest;
import com.mopl.domain.notification.dto.NotificationSortBy;
import com.mopl.global.config.QueryDslConfig;
import com.mopl.global.dto.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import({QueryDslConfig.class, NotificationRepositoryCustomImpl.class})
@DisplayName("NotificationRepositoryCustomImpl 테스트")
class NotificationRepositoryCustomImplTest {

  @Autowired
  private NotificationRepository notificationRepository;

  private UUID receiverId;

  @BeforeEach
  void setUp() {
    receiverId = UUID.randomUUID();
  }
  @PersistenceContext
  private EntityManager em;

  private Notification saveNotification(
      UUID receiverId,
      NotificationType type,
      Instant createdAt,
      boolean isRead
  ) {
    Notification notification = Notification.builder()
        .eventId(UUID.randomUUID())
        .receiverId(receiverId)
        .type(type)
        .title("title")
        .content("content")
        .isRead(isRead)
        .createdAt(createdAt)
        .build();
    Notification saved = notificationRepository.save(notification);

    em.createQuery("UPDATE Notification n SET n.createdAt = :createdAt WHERE n.id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", saved.getId())
            .executeUpdate();
    em.flush();
    em.clear();

    return notificationRepository.findById(saved.getId()).orElseThrow();
  }

  @Test
  @DisplayName("성공: 미읽음 알림을 커서 없이 조회한다")
  void findUnreadByReceiverWithCursor_withoutCursor() {
    Instant base = Instant.parse("2026-01-01T00:00:00Z");
    saveNotification(receiverId, NotificationType.FOLLOW, base, false);
    saveNotification(receiverId, NotificationType.FOLLOW, base.plusSeconds(1), false);
    saveNotification(receiverId, NotificationType.FOLLOW, base.plusSeconds(2), true);

    NotificationSearchRequest request = new NotificationSearchRequest(
        null, null, 10, SortDirection.ASCENDING, NotificationSortBy.createdAt
    );

    List<Notification> result =
        notificationRepository.findUnreadByReceiverWithCursor(receiverId, request);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("성공: ASC 커서 이후 알림을 조회한다")
  void findUnreadByReceiverWithCursor_ascCursor() {
    Instant base = Instant.parse("2026-01-01T00:00:00Z");
    Notification first = saveNotification(receiverId, NotificationType.FOLLOW, base, false);
    Notification second = saveNotification(receiverId, NotificationType.FOLLOW, base.plusSeconds(1), false);

    NotificationSearchRequest request = new NotificationSearchRequest(
        first.getCreatedAt().toString(),
        first.getId(),
        10,
        SortDirection.ASCENDING,
        NotificationSortBy.createdAt
    );

    List<Notification> result =
        notificationRepository.findUnreadByReceiverWithCursor(receiverId, request);

    assertThat(result).extracting(Notification::getId).containsExactly(second.getId());
  }

  @Test
  @DisplayName("성공: DESC 커서 이후 알림을 조회한다")
  void findUnreadByReceiverWithCursor_descCursor() {
    Instant base = Instant.parse("2026-01-01T00:00:00Z");
    Notification first = saveNotification(receiverId, NotificationType.FOLLOW, base, false);
    Notification second = saveNotification(receiverId, NotificationType.FOLLOW, base.plusSeconds(1), false);

    NotificationSearchRequest request = new NotificationSearchRequest(
        second.getCreatedAt().toString(),
        second.getId(),
        10,
        SortDirection.DESCENDING,
        NotificationSortBy.createdAt
    );

    List<Notification> result =
        notificationRepository.findUnreadByReceiverWithCursor(receiverId, request);

    assertThat(result).extracting(Notification::getId).containsExactly(first.getId());
  }

  @Test
  @DisplayName("성공: lastNotificationId 이후 알림을 replay용으로 조회한다")
  void findByReceiverIdAfter_returnsLaterNotifications() {
    Instant base = Instant.parse("2026-01-01T00:00:00Z");
    Notification first = saveNotification(receiverId, NotificationType.FOLLOW, base, false);
    Notification second = saveNotification(receiverId, NotificationType.FOLLOW, base.plusSeconds(1), false);

    List<Notification> result =
        notificationRepository.findByReceiverIdAfter(receiverId, first.getId());

    assertThat(result).extracting(Notification::getId).containsExactly(second.getId());
  }

  @Test
  @DisplayName("성공: 존재하지 않는 lastNotificationId면 빈 목록을 반환한다")
  void findByReceiverIdAfter_unknownLastId() {
    List<Notification> result =
        notificationRepository.findByReceiverIdAfter(receiverId, UUID.randomUUID());

    assertThat(result).isEmpty();
  }
}