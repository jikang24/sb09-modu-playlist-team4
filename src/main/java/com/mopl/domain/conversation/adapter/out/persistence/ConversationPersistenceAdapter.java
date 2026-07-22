package com.mopl.domain.conversation.adapter.out.persistence;


import com.mopl.domain.conversation.application.dto.ConversationSearchCondition;
import com.mopl.domain.conversation.application.port.out.LoadConversationPort;
import com.mopl.domain.conversation.application.port.out.LoadUserPort;
import com.mopl.domain.conversation.application.port.out.SaveConversationPort;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.domain.dm.application.port.in.GetConversationIdsByContentUseCase;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.response.CursorPageResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationPersistenceAdapter implements SaveConversationPort, LoadConversationPort {

  private final ConversationRepository conversationRepository;
  private final ConversationPersistenceMapper mapper;
  private final JPAQueryFactory queryFactory;
  private final GetConversationIdsByContentUseCase getConversationIdsByContentUseCase;
  private final LoadUserPort loadUserPort;

  /**
   * Conversation은 생성 후 수정/삭제 메서드가 없는 불변 데이터라 evict 대상이 없다 -
   * TTL 만료로만 자연스럽게 갱신된다.
   */
  @Override
  @Cacheable(value = "conversation", key = "#id")
  public Optional<Conversation> findById(UUID id) {
    return conversationRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Conversation> findByParticipants(UUID myId, UUID withUserId) {
   QConversationJpaEntity c = QConversationJpaEntity.conversationJpaEntity;
    ConversationJpaEntity result = queryFactory
        .selectFrom(c)
        .where(
            (c.participant1Id.eq(myId).and(c.participant2Id.eq(withUserId)))
                .or(c.participant1Id.eq(withUserId).and(c.participant2Id.eq(myId)))
        )
        .fetchFirst();
    return Optional.ofNullable(mapper.toDomain(result));
  }

  @Override
  public CursorPageResponse<Conversation> findList(UUID myId,
      ConversationSearchCondition condition) {
    QConversationJpaEntity c = QConversationJpaEntity.conversationJpaEntity;
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(c.participant1Id.eq(myId).or(c.participant2Id.eq(myId)));

    if (condition.hasKeyword()) {
      List<UUID> conversationIds = getConversationIdsByContentUseCase
          .findConversationIdsByContent(condition.keywordLike());
      List<UUID> userIdsByName = loadUserPort.findUserIdsByNameLike(condition.keywordLike());

      if (conversationIds.isEmpty() && userIdsByName.isEmpty()) {
        return new CursorPageResponse<>(
            List.of(), null, null, false, 0,
            condition.sortBy(), condition.sortDirection().name()
        );
      }

      BooleanBuilder keywordBuilder = new BooleanBuilder();
      if (!conversationIds.isEmpty()) {
        keywordBuilder.or(c.id.in(conversationIds));
      }
      if (!userIdsByName.isEmpty()) {
        keywordBuilder.or(c.participant1Id.in(userIdsByName).or(c.participant2Id.in(userIdsByName)));
      }

      builder.and(keywordBuilder);
    }

    boolean ascending = condition.sortDirection() == SortDirection.ASCENDING;

    if (!condition.hasFirstPage()) {
      Instant cursorTime = Instant.parse(condition.cursor());
      if (condition.idAfter() != null) {
        // 같은 밀리초에 여러 대화가 생성될 수 있어 createdAt만으로는 페이지 경계에서
        // 동시각 데이터가 누락/중복될 수 있다 - id로 동시각 타이브레이크
        BooleanBuilder cursorBuilder = new BooleanBuilder();
        if (ascending) {
          cursorBuilder.or(c.createdAt.gt(cursorTime));
          cursorBuilder.or(c.createdAt.eq(cursorTime).and(c.id.gt(condition.idAfter())));
        } else {
          cursorBuilder.or(c.createdAt.lt(cursorTime));
          cursorBuilder.or(c.createdAt.eq(cursorTime).and(c.id.lt(condition.idAfter())));
        }
        builder.and(cursorBuilder);
      } else if (ascending) {
        builder.and(c.createdAt.gt(cursorTime));
      } else {
        builder.and(c.createdAt.lt(cursorTime));
      }
    }

    List<ConversationJpaEntity> results = queryFactory
        .selectFrom(c)
        .where(builder)
        .orderBy(ascending ? c.createdAt.asc() : c.createdAt.desc(),
            ascending ? c.id.asc() : c.id.desc())
        .limit(condition.limit() + 1)
        .fetch();

    boolean hasNext = results.size() > condition.limit();
    if (hasNext) {
      results = results.subList(0, condition.limit());
    }

    List<Conversation> data = results.stream()
        .map(mapper::toDomain)
        .toList();

    String nexCursor = hasNext && !data.isEmpty()
        ? data.get(data.size() - 1).getCreatedAt().toString()
        : null;

    UUID nextIdAfter = hasNext && !data.isEmpty()
        ? data.get(data.size() - 1).getId()
        : null;

    return new CursorPageResponse<>(data, nexCursor, nextIdAfter, hasNext, data.size(),
        condition.sortBy(), condition.sortDirection().name());
  }

  @Override
  public Conversation save(Conversation conversation) {
    ConversationJpaEntity conversationJpaEntity = mapper.toJpaEntity(conversation);
    ConversationJpaEntity savedConversation = conversationRepository.save(conversationJpaEntity);
    return mapper.toDomain(savedConversation);
  }
}
