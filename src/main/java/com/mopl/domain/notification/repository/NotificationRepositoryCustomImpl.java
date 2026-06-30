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
                .and(notification.id.gt(request.idAfter()))));
      }
    }

    OrderSpecifier<?> createdAtOrder = request.sortDirection() == SortDirection.ASCENDING
        ? notification.createdAt.asc()
        : notification.createdAt.desc();

    return queryFactory.selectFrom(notification)
        .where(builder)
        .orderBy(createdAtOrder, notification.id.asc())
        .limit(request.limit() + 1L)
        .fetch();
  }

  @Override
  public long countUnreadByReceiver(UUID receiverId) {
    QNotification notification = QNotification.notification;
    Long count = queryFactory
        .select(notification.count())
        .from(notification)
        .where(unreadReceiverFilter(notification, receiverId))
        .fetchOne();

    return count != null ? count : 0L;
  }

  private BooleanBuilder unreadReceiverFilter(QNotification notification, UUID receiverId) {
    return new BooleanBuilder()
        .and(notification.receiverId.eq(receiverId))
        .and(notification.isRead.isFalse());
  }
}
