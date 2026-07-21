package com.mopl.domain.conversation.adapter.in.web.mapper;

import com.mopl.domain.conversation.adapter.in.web.dto.ConversationDto;
import com.mopl.domain.conversation.application.port.out.LoadUserPort;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.domain.dm.application.port.in.CheckUnreadDirectMessageUseCase;
import com.mopl.domain.dm.application.port.in.GetLatestDirectMessageUseCase;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.UserSummary;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  /**
   * 목록 조회 전용 - 대화 건수만큼 개별 조회하지 않고, 목록 전체에 필요한
   * 유저 정보/최근 메시지/읽음여부를 각각 한 번씩만 배치 조회해서 조립한다 (N+1 방지)
   */
  public List<ConversationDto> toDtoList(List<Conversation> conversations, UUID myId) {
    if (conversations.isEmpty()) {
      return Collections.emptyList();
    }

    ConversationEnrichment enrichment = loadEnrichment(conversations, myId);

    return conversations.stream()
        .map(conversation -> toDto(conversation, myId, enrichment))
        .toList();
  }

  private ConversationEnrichment loadEnrichment(List<Conversation> conversations, UUID myId) {
    List<UUID> conversationIds = conversations.stream().map(Conversation::getId).toList();

    Map<UUID, DirectMessage> latestMessages = getLatestDirectMessageUseCase.getLatestBulk(conversationIds);

    Set<UUID> userIds = new HashSet<>();
    conversations.forEach(c -> userIds.add(c.getOtherParticipant(myId)));
    latestMessages.values().forEach(dm -> {
      userIds.add(dm.getSenderId());
      userIds.add(dm.getReceiverId());
    });
    Map<UUID, UserSummary> users = loadUserPort.getUserSummaries(userIds);

    Set<UUID> unreadConversationIds = checkUnreadDirectMessageUseCase.hasUnreadBulk(conversationIds, myId);

    return new ConversationEnrichment(users, latestMessages, unreadConversationIds);
  }

  private ConversationDto toDto(Conversation conversation, UUID myId, ConversationEnrichment enrichment) {
    UUID otherParticipantId = conversation.getOtherParticipant(myId);
    UserSummary with = enrichment.users().get(otherParticipantId);

    DirectMessage latest = enrichment.latestMessages().get(conversation.getId());
    DirectMessageDto lastestMessage = latest == null ? null : new DirectMessageDto(
        latest.getId(),
        latest.getConversationId(),
        latest.getCreatedAt(),
        enrichment.users().get(latest.getSenderId()),
        enrichment.users().get(latest.getReceiverId()),
        latest.getContent()
    );

    boolean hasUnread = enrichment.unreadConversationIds().contains(conversation.getId());

    return new ConversationDto(
        conversation.getId(),
        with,
        lastestMessage,
        hasUnread
    );
  }

  private record ConversationEnrichment(
      Map<UUID, UserSummary> users,
      Map<UUID, DirectMessage> latestMessages,
      Set<UUID> unreadConversationIds
  ) {
  }

}
