package com.mopl.domain.dm.adapter.out.persistence;

import com.mopl.domain.dm.application.dto.DirectMessageSearchCondition;
import com.mopl.domain.dm.application.port.out.LoadDirectMessagePort;
import com.mopl.domain.dm.application.port.out.SaveDirectMessagePort;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.response.CursorPageResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(dm.conversationId.eq(conversationId));
    if(!condition.isFirstPage()){
      Instant cursorTime = Instant.parse(condition.cursor());
      if(condition.sortDirection().equals(SortDirection.ASCENDING)){
        builder.and(dm.createdAt.gt(cursorTime));
      }else {
        builder.and(dm.createdAt.lt(cursorTime));
      }
    }

    var orderSpecifier = condition.sortDirection().equals(SortDirection.ASCENDING) ? dm.createdAt.asc() : dm.createdAt.desc();

    List<DirectMessageJpaEntity> results = queryFactory
        .selectFrom(dm)
        .where(builder)
        .orderBy(orderSpecifier)
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
  public List<UUID> findConversationIdsByContent(String keyword) {
    QDirectMessageJpaEntity dm = QDirectMessageJpaEntity.directMessageJpaEntity;
    return queryFactory.select(dm.conversationId)
        .from(dm)
        .where(dm.content.containsIgnoreCase(keyword))
        .distinct()
        .fetch();
  }
}
