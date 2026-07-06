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
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@RequiredArgsConstructor
public class ConversationPersistenceAdapter implements SaveConversationPort, LoadConversationPort {

  private final ConversationRepository conversationRepository;
  private final ConversationPersistenceMapper mapper;
  private final JPAQueryFactory queryFactory;
  private final GetConversationIdsByContentUseCase getConversationIdsByContentUseCase;
  private final LoadUserPort loadUserPort;

  @Override
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

    if (!condition.hasFirstPage()) {
      Instant cursorTime = Instant.parse(condition.cursor());
      if (condition.sortDirection() == SortDirection.ASCENDING) {
        builder.and(c.createdAt.gt(cursorTime));
      } else {
        builder.and(c.createdAt.lt(cursorTime));
      }
    }

    var orderSpecifier = condition.sortDirection() == SortDirection.ASCENDING
        ? c.createdAt.asc()
        : c.createdAt.desc();

    List<ConversationJpaEntity> results = queryFactory
        .selectFrom(c)
        .where(builder)
        .orderBy(orderSpecifier)
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
