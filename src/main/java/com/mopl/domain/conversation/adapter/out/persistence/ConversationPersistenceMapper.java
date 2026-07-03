package com.mopl.domain.conversation.adapter.out.persistence;

import com.mopl.domain.conversation.domain.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversationPersistenceMapper {

  @Mapping(target = "participant1Id", source = "participant1Id")
  @Mapping(target = "participant2Id", source = "participant2Id")
  ConversationJpaEntity toJpaEntity(Conversation conversation);
  @Mapping(target = "participants", ignore = true)
  Conversation toDomain(ConversationJpaEntity conversationJpaEntity);


}
