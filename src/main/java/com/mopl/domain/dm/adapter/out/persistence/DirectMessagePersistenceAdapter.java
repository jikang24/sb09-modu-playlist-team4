package com.mopl.domain.dm.adapter.out.persistence;

import com.mopl.domain.dm.application.dto.DirectMessageSearchCondition;
import com.mopl.domain.dm.application.port.out.LoadDirectMessagePort;
import com.mopl.domain.dm.application.port.out.SaveDirectMessagePort;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.response.CursorPageResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@RequiredArgsConstructor
public class DirectMessagePersistenceAdapter implements SaveDirectMessagePort,
    LoadDirectMessagePort {
  private final DirectMessageRepository directMessageRepository;
  private final DirectMessagePersistenceMapper mapper;
  private final JPAQueryFactory queryFactory;

  @Override
  public Optional<DirectMessage> findById(UUID directMessageId) {
    return directMessageRepository.findById(directMessageId).map(mapper::toDomain);
  }

  @Override
  public Optional<DirectMessage> findLatestByConversationId(UUID conversationId) {
    QDirectMessageJpaEntity dm = QDirectMessageJpaEntity.directMessageJpaEntity;
    DirectMessageJpaEntity result = queryFactory.selectFrom(dm)
        .where(dm.conversationId.eq(conversationId))
        .orderBy(dm.createdAt.desc())
        .fetchFirst();
    return Optional.ofNullable(mapper.toDomain(result));
  }

  @Override
  public boolean hasUnRead(UUID conversationId, UUID myId) {
    QDirectMessageJpaEntity dm = QDirectMessageJpaEntity.directMessageJpaEntity;
    Long count = queryFactory
        .select(dm.count())
        .from(dm)
        .where(dm.conversationId.eq(conversationId),
            dm.receiverId.eq(myId),
            dm.read.eq(false)
        ).fetchOne();
    return count != null && count > 0;
  }

  @Override
  public CursorPageResponse<DirectMessage> findList(UUID conversationId,
      DirectMessageSearchCondition condition) {
    QDirectMessageJpaEntity dm = QDirectMessageJpaEntity.directMessageJpaEntity;
    boolean ascending = condition.sortDirection().equals(SortDirection.ASCENDING);

    BooleanBuilder builder = new BooleanBuilder();
    builder.and(dm.conversationId.eq(conversationId));
    if(!condition.isFirstPage()){
      Instant cursorTime = Instant.parse(condition.cursor());
      if (condition.idAfter() != null) {
        // 같은 밀리초에 여러 메시지가 저장될 수 있어 createdAt만으로는 페이지 경계에서
        // 동시각 메시지가 누락/중복될 수 있다 - id로 동시각 타이브레이크
        BooleanBuilder cursorBuilder = new BooleanBuilder();
        if (ascending) {
          cursorBuilder.or(dm.createdAt.gt(cursorTime));
          cursorBuilder.or(dm.createdAt.eq(cursorTime).and(dm.id.gt(condition.idAfter())));
        } else {
          cursorBuilder.or(dm.createdAt.lt(cursorTime));
          cursorBuilder.or(dm.createdAt.eq(cursorTime).and(dm.id.lt(condition.idAfter())));
        }
        builder.and(cursorBuilder);
      } else if (ascending) {
        builder.and(dm.createdAt.gt(cursorTime));
      } else {
        builder.and(dm.createdAt.lt(cursorTime));
      }
    }

    List<DirectMessageJpaEntity> results = queryFactory
        .selectFrom(dm)
        .where(builder)
        .orderBy(ascending ? dm.createdAt.asc() : dm.createdAt.desc(),
            ascending ? dm.id.asc() : dm.id.desc())
        .limit(condition.limit()+1)
        .fetch();
    boolean hasNext = results.size() > condition.limit();
    if(hasNext){
      results = results.subList(0, condition.limit());

    }
    List<DirectMessage> data = results.stream()
        .map(mapper::toDomain)
        .toList();

    String nexCursor = hasNext && !data.isEmpty()
        ? data.get(data.size()-1).getCreatedAt().toString()
        : null;

    UUID nextIdAfter = hasNext && !data.isEmpty()
        ? data.get(data.size()-1).getId()
        : null;

    return new CursorPageResponse<>(
        data,
        nexCursor,
        nextIdAfter,
        hasNext,
        data.size(),
        condition.sortBy(),
        condition.sortDirection().name()
    );
  }

  @Override
  public DirectMessage save(DirectMessage directMessage) {
    DirectMessageJpaEntity directMessageJpaEntity = mapper.toJpaEntity(directMessage);
    DirectMessageJpaEntity savedDirectMessage = directMessageRepository.save(directMessageJpaEntity);
    return mapper.toDomain(savedDirectMessage);
  }

  @Override
  public void markAllAsReadUpTo(UUID conversationId, UUID receiverId, Instant upToCreatedAt) {
    QDirectMessageJpaEntity dm = QDirectMessageJpaEntity.directMessageJpaEntity;
    queryFactory.update(dm)
        .set(dm.read, true)
        .where(
            dm.conversationId.eq(conversationId),
            dm.receiverId.eq(receiverId),
            dm.read.eq(false),
            dm.createdAt.loe(upToCreatedAt)
        )
        .execute();
  }

  @Override
  public List<UUID> findConversationIdsByContent(String keyword) {
    QDirectMessageJpaEntity dm = QDirectMessageJpaEntity.directMessageJpaEntity;
    return queryFactory.select(dm.conversationId)
        .from(dm)
        .where(dm.content.containsIgnoreCase(keyword))
        .distinct()
        .fetch();
  }

  @Override
  public Map<UUID, DirectMessage> findLatestByConversationIds(Collection<UUID> conversationIds) {
    if (conversationIds.isEmpty()) {
      return Map.of();
    }
    QDirectMessageJpaEntity dm = QDirectMessageJpaEntity.directMessageJpaEntity;
    var maxCreatedAt = dm.createdAt.max();

    // 1) 대화방별 최신 시각을 한 번에 조회 (conversationId 개수와 무관하게 쿼리 1번)
    List<Tuple> latestTimes = queryFactory
        .select(dm.conversationId, maxCreatedAt)
        .from(dm)
        .where(dm.conversationId.in(conversationIds))
        .groupBy(dm.conversationId)
        .fetch();

    if (latestTimes.isEmpty()) {
      return Map.of();
    }

    // 2) (conversationId, 최신 시각) 쌍으로 실제 메시지 엔티티를 한 번에 조회
    BooleanBuilder condition = new BooleanBuilder();
    for (Tuple t : latestTimes) {
      UUID conversationId = t.get(dm.conversationId);
      Instant latestCreatedAt = t.get(maxCreatedAt);
      condition.or(dm.conversationId.eq(conversationId).and(dm.createdAt.eq(latestCreatedAt)));
    }

    List<DirectMessageJpaEntity> latestEntities = queryFactory
        .selectFrom(dm)
        .where(condition)
        .fetch();

    return latestEntities.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toMap(
            DirectMessage::getConversationId,
            message -> message,
            (existing, duplicate) -> existing
        ));
  }

  @Override
  public Set<UUID> findConversationIdsWithUnread(Collection<UUID> conversationIds, UUID myId) {
    if (conversationIds.isEmpty()) {
      return Set.of();
    }
    QDirectMessageJpaEntity dm = QDirectMessageJpaEntity.directMessageJpaEntity;

    List<UUID> unreadConversationIds = queryFactory
        .select(dm.conversationId)
        .from(dm)
        .where(
            dm.conversationId.in(conversationIds),
            dm.receiverId.eq(myId),
            dm.read.eq(false)
        )
        .distinct()
        .fetch();

    return new HashSet<>(unreadConversationIds);
  }
}
