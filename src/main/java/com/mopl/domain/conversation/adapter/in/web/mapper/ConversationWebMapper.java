package com.mopl.domain.conversation.adapter.in.web.mapper;

import com.mopl.domain.conversation.adapter.in.web.dto.ConversationDto;
import com.mopl.domain.conversation.application.port.out.LoadUserPort;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.domain.dm.application.port.in.CheckUnreadDirectMessageUseCase;
import com.mopl.domain.dm.application.port.in.GetLatestDirectMessageUseCase;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.UserSummary;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationWebMapper {
  private final LoadUserPort loadUserPort;
  private final GetLatestDirectMessageUseCase getLatestDirectMessageUseCase;
  private final CheckUnreadDirectMessageUseCase checkUnreadDirectMessageUseCase;

  public ConversationDto toDto(Conversation conversation, UUID myId) {
    UUID otherParticipantId = conversation.getOtherParticipant(myId);
    UserSummary with = loadUserPort.getUserSummary(otherParticipantId);

    DirectMessageDto lastestMessage = getLatestDirectMessageUseCase
        .getLatest(conversation.getId())
        .map(dm -> {
          UserSummary sender = loadUserPort.getUserSummary(dm.getSenderId());
          UserSummary receiver = loadUserPort.getUserSummary(dm.getReceiverId());
          return new DirectMessageDto(
              dm.getId(),
              dm.getConversationId(),
              dm.getCreatedAt(),
              sender,
              receiver,
              dm.getContent()
          );
        })
        .orElse(null);

    boolean hasUnread = checkUnreadDirectMessageUseCase.hasUnread(conversation.getId(),myId);
    return new ConversationDto(
        conversation.getId(),
        with,
        lastestMessage,
        hasUnread
    );
  }

}
