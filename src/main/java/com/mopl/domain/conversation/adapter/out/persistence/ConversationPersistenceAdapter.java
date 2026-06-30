package com.mopl.domain.conversation.adapter.out.persistence;


import com.mopl.domain.conversation.application.dto.ConversationSearchCondition;
import com.mopl.domain.conversation.application.port.out.LoadConversationPort;
import com.mopl.domain.conversation.application.port.out.SaveConversationPort;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.global.response.CursorPageResponse;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
        .fetchOne();
    return Optional.ofNullable(mapper.toDomain(result));
  }

  @Override
//  TODO: keywordLike 검색은 DirectMessage 모듈 구현 후 추가 예정
//  현재는 페이지네이션 기본 로직만 구성.
  public CursorPageResponse<Conversation> findList(UUID myId,
      ConversationSearchCondition condition) {
    return null;
  }

  @Override
  public Conversation save(Conversation conversation) {
    ConversationJpaEntity conversationJpaEntity = mapper.toJpaEntity(conversation);
    ConversationJpaEntity savedConversation = conversationRepository.save(conversationJpaEntity);
    return mapper.toDomain(savedConversation);
  }
}
