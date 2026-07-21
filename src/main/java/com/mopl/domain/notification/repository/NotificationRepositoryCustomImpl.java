package com.mopl.domain.notification.repository;

import com.mopl.domain.notification.domain.Notification;
import com.mopl.domain.notification.domain.QNotification;
import com.mopl.domain.notification.dto.NotificationSearchRequest;
import com.mopl.global.dto.SortDirection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Notification> findUnreadByReceiverWithCursor(
      UUID receiverId,
      NotificationSearchRequest request
  ) {
    QNotification notification = QNotification.notification;
    BooleanBuilder builder = unreadReceiverFilter(notification, receiverId);

    if (request.cursor() != null && request.idAfter() != null) {
      Instant cursorCreatedAt = Instant.parse(request.cursor());
      if (request.sortDirection() == SortDirection.ASCENDING) {
        builder.and(notification.createdAt.gt(cursorCreatedAt)
            .or(notification.createdAt.eq(cursorCreatedAt)
                .and(notification.id.gt(request.idAfter()))));
      } else {
        builder.and(notification.createdAt.lt(cursorCreatedAt)
            .or(notification.createdAt.eq(cursorCreatedAt)
                .and(notification.id.lt(request.idAfter()))));
      }
    }

    OrderSpecifier<?> createdAtOrder = request.sortDirection() == SortDirection.ASCENDING
        ? notification.createdAt.asc()
        : notification.createdAt.desc();
    OrderSpecifier<?> idOrder = request.sortDirection() == SortDirection.ASCENDING
        ? notification.id.asc()
        : notification.id.desc();

    return queryFactory.selectFrom(notification)
        .where(builder)
        .orderBy(createdAtOrder, idOrder)
        .limit(request.limit() + 1L)
        .fetch();
  }

  @Override
  public List<Notification> findByReceiverIdAfter(UUID receiverId, UUID lastNotificationId, int limit) {
    QNotification notification = QNotification.notification;

    Notification lastNotification = queryFactory.selectFrom(notification)
        .where(notification.receiverId.eq(receiverId)
            .and(notification.id.eq(lastNotificationId)))
        .fetchOne();

    if (lastNotification == null) {
      return List.of();
    }

    return queryFactory.selectFrom(notification)
        .where(notification.receiverId.eq(receiverId)
            .and(notification.createdAt.gt(lastNotification.getCreatedAt())
                .or(notification.createdAt.eq(lastNotification.getCreatedAt())
                    .and(notification.id.gt(lastNotification.getId())))))
        .orderBy(notification.createdAt.asc(), notification.id.asc())
        .limit(limit)
        .fetch();
  }

  private BooleanBuilder unreadReceiverFilter(QNotification notification, UUID receiverId) {
    return new BooleanBuilder()
        .and(notification.receiverId.eq(receiverId))
        .and(notification.isRead.isFalse());
  }
}
