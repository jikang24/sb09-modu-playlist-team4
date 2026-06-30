package com.mopl.domain.conversation.application.service;

import com.mopl.domain.conversation.application.dto.ConversationSearchCondition;
import com.mopl.domain.conversation.application.port.in.CreateConversationUseCase;
import com.mopl.domain.conversation.application.port.in.GetConversationListUseCase;
import com.mopl.domain.conversation.application.port.in.GetConversationUseCase;
import com.mopl.domain.conversation.application.port.out.LoadConversationPort;
import com.mopl.domain.conversation.application.port.out.SaveConversationPort;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConversationService implements CreateConversationUseCase, GetConversationUseCase ,
    GetConversationListUseCase {
  private final SaveConversationPort saveConversationPort;
  private final LoadConversationPort loadConversationPort;

  @Override
  public Conversation create(UUID userId1, UUID userId2) {
   Conversation conversation = Conversation.create(userId1, userId2);
   log.info("conversation created {}",conversation);
   return saveConversationPort.save(conversation);
  }

  @Override
  public Conversation getById(UUID conversationId, UUID myId) {
    Conversation conversation = loadConversationPort.findById(conversationId)
        .orElseThrow(()-> {
              log.warn("Conversation not found with id: {}", conversationId);
               return new MoplException(ErrorCode.CONVERSATION_NOT_FOUND);
            });
    if(!conversation.hasParticipant(myId)){
      log.warn("User {} is not participant of conversation {}",myId,conversationId);
      throw new MoplException(ErrorCode.FORBIDDEN_ACCESS);
    }
    return conversation;
  }

  @Override
  public Conversation getByParticipant(UUID myId, UUID withUserId) {
    Conversation conversation = loadConversationPort.findByParticipants(myId,withUserId)
        .orElseThrow(()->
        {
          log.warn("Conversation not found with participant: {}", withUserId);
          return new MoplException(ErrorCode.CONVERSATION_NOT_FOUND);
        });
    return conversation;
  }

  @Override
  public CursorPageResponse<Conversation> getList(UUID myId,
      ConversationSearchCondition conversationSearchCondition) {
    log.info("get conversation list");
    return loadConversationPort.findList(myId,conversationSearchCondition);
  }
}
