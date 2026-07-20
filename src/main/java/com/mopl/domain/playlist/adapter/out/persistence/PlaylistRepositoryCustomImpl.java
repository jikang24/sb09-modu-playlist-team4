package com.mopl.domain.playlist.adapter.out.persistence;

import com.mopl.domain.playlist.application.dto.PlaylistSearchCondition;
import com.mopl.domain.playlist.application.dto.SortBy;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaylistRepositoryCustomImpl implements PlaylistRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<PlaylistJpaEntity> findAllWithCursor(PlaylistSearchCondition condition) {
    QPlaylistJpaEntity p = QPlaylistJpaEntity.playlistJpaEntity;
    QPlaylistSubscriptionJpaEntity s =
        QPlaylistSubscriptionJpaEntity.playlistSubscriptionJpaEntity;

    BooleanBuilder whereBuilder = buildFilter(p, s, condition);
    BooleanExpression updatedAtCursor = buildUpdatedAtCursor(condition, p);
    if (updatedAtCursor != null) {
      whereBuilder.and(updatedAtCursor);
    }

    NumberExpression<Long> subscribeCount = s.id.countDistinct();
    BooleanExpression subscribeCountCursor = buildSubscribeCountCursor(condition, p, subscribeCount);

    OrderSpecifier<?> primaryOrder = buildOrder(condition, p, subscribeCount);
    OrderSpecifier<?> idOrder = condition.sortDirection() == SortDirection.ASCENDING
        ? p.id.asc()
        : p.id.desc();

    return queryFactory
        .selectFrom(p)
        .leftJoin(p.subscriptions, s)
        .where(whereBuilder)
        .groupBy(p.id)
        .having(subscribeCountCursor)
        .orderBy(primaryOrder, idOrder)
        .limit(condition.limit() + 1L)
        .fetch();
  }

  @Override
  public List<PlaylistContentJpaEntity> findContentsByPlaylistIds(List<UUID> playlistIds) {
    if (playlistIds == null || playlistIds.isEmpty()) {
      return List.of();
    }
    QPlaylistContentJpaEntity pc = QPlaylistContentJpaEntity.playlistContentJpaEntity;

    return queryFactory
        .selectFrom(pc)
        .where(pc.playlist.id.in(playlistIds))
        .orderBy(pc.position.asc())
        .fetch();
  }

  @Override
  public void deleteByContentId(UUID contentId) {
    QPlaylistContentJpaEntity pc = QPlaylistContentJpaEntity.playlistContentJpaEntity;

    queryFactory
        .delete(pc)
        .where(pc.contentId.eq(contentId))
        .execute();
  }

  @Override
  public long countAll(PlaylistSearchCondition condition) {
    QPlaylistJpaEntity p = QPlaylistJpaEntity.playlistJpaEntity;
    QPlaylistSubscriptionJpaEntity s =
        QPlaylistSubscriptionJpaEntity.playlistSubscriptionJpaEntity;

    BooleanBuilder builder = buildFilter(p, s, condition);

    Long count = queryFactory
        .select(p.id.countDistinct())
        .from(p)
        .leftJoin(p.subscriptions, s)
        .where(builder)
        .fetchOne();

    return count == null ? 0L : count;
  }

  private BooleanBuilder buildFilter(
      QPlaylistJpaEntity p,
      QPlaylistSubscriptionJpaEntity s,
      PlaylistSearchCondition condition
  ) {
    BooleanBuilder builder = new BooleanBuilder();

    if (condition.keywordLike() != null && !condition.keywordLike().isBlank()) {
      builder.and(
          p.title.containsIgnoreCase(condition.keywordLike())
              .or(p.description.containsIgnoreCase(condition.keywordLike()))
      );
    }

    if (condition.ownerIdEqual() != null) {
      builder.and(p.ownerId.eq(condition.ownerIdEqual()));
    }

    if (condition.subscriberIdEqual() != null) {
      builder.and(s.subscriberId.eq(condition.subscriberIdEqual()));
    }

    return builder;
  }

  private OrderSpecifier<?> buildOrder(
      PlaylistSearchCondition condition,
      QPlaylistJpaEntity p,
      NumberExpression<Long> subscribeCount
  ) {
    boolean asc = condition.sortDirection() == SortDirection.ASCENDING;

    return switch (condition.sortBy()) {
      case updatedAt -> asc ? p.updatedAt.asc() : p.updatedAt.desc();
      case subscribeCount -> asc ? subscribeCount.asc() : subscribeCount.desc();
    };
  }

  private BooleanExpression buildUpdatedAtCursor(
      PlaylistSearchCondition condition,
      QPlaylistJpaEntity p
  ) {
    if (condition.sortBy() != SortBy.updatedAt) {
      return null;
    }
    if (condition.cursor() == null || condition.cursor().isBlank()
        || condition.idAfter() == null) {
      return null;
    }

    Instant cursor = parseInstantCursor(condition.cursor());

    if (condition.sortDirection() == SortDirection.ASCENDING) {
      return p.updatedAt.gt(cursor)
          .or(p.updatedAt.eq(cursor).and(p.id.gt(condition.idAfter())));
    }
    return p.updatedAt.lt(cursor)
        .or(p.updatedAt.eq(cursor).and(p.id.lt(condition.idAfter())));
  }

  private BooleanExpression buildSubscribeCountCursor(
      PlaylistSearchCondition condition,
      QPlaylistJpaEntity p,
      NumberExpression<Long> subscribeCount
  ) {
    if (condition.sortBy() != SortBy.subscribeCount) {
      return null;
    }
    if (condition.cursor() == null || condition.cursor().isBlank()
        || condition.idAfter() == null) {
      return null;
    }

    long cursor = parseLongCursor(condition.cursor());

    if (condition.sortDirection() == SortDirection.ASCENDING) {
      return subscribeCount.gt(cursor)
          .or(subscribeCount.eq(cursor).and(p.id.gt(condition.idAfter())));
    }
    return subscribeCount.lt(cursor)
        .or(subscribeCount.eq(cursor).and(p.id.lt(condition.idAfter())));
  }

  private Instant parseInstantCursor(String cursor) {
    try {
      return Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
    }
  }

  private long parseLongCursor(String cursor) {
    try {
      return Long.parseLong(cursor);
    } catch (NumberFormatException e) {
      throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
    }
  }
}
