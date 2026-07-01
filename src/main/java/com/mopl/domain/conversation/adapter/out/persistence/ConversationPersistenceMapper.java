package com.mopl.domain.conversation.adapter.out.persistence;

import com.mopl.domain.conversation.domain.Conversation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationPersistenceMapper {
  ConversationJpaEntity toJpaEntity(Conversation conversation);
  Conversation toDomain(ConversationJpaEntity conversationJpaEntity);


}
