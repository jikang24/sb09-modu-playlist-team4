package com.mopl.domain.dm.application.service;

import com.mopl.domain.dm.application.dto.DirectMessageSearchCondition;
import com.mopl.domain.dm.application.port.in.CheckUnreadDirectMessageUseCase;
import com.mopl.domain.dm.application.port.in.GetConversationIdsByContentUseCase;
import com.mopl.domain.dm.application.port.in.GetDirectMessageListUseCase;
import com.mopl.domain.dm.application.port.in.GetLatestDirectMessageUseCase;
import com.mopl.domain.dm.application.port.in.ReadDirectMessageUseCase;
import com.mopl.domain.dm.application.port.in.SendDirectMessageUseCase;
import com.mopl.domain.dm.application.port.out.LoadDirectMessagePort;
import com.mopl.domain.dm.application.port.out.LoadUserPort;
import com.mopl.domain.dm.application.port.out.SaveDirectMessagePort;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DirectMessageService implements CheckUnreadDirectMessageUseCase,
    SendDirectMessageUseCase, GetLatestDirectMessageUseCase, GetDirectMessageListUseCase,
    ReadDirectMessageUseCase, GetConversationIdsByContentUseCase {
  private final LoadDirectMessagePort loadDirectMessagePort;
  private final SaveDirectMessagePort saveDirectMessagePort;
  private final LoadUserPort loadUserPort;

  @Override
  public boolean hasUnread(UUID conversationId, UUID myId) {

   return loadDirectMessagePort.hasUnRead(conversationId, myId);
  }

  @Override
  public Set<UUID> hasUnreadBulk(Collection<UUID> conversationIds, UUID myId) {
    if (conversationIds.isEmpty()) {
      return Set.of();
    }
    return loadDirectMessagePort.findConversationIdsWithUnread(conversationIds, myId);
  }

  @Override
  public Optional<DirectMessage> getLatest(UUID conversationId) {
    return loadDirectMessagePort.findLatestByConversationId(conversationId);
  }

  @Override
  public Map<UUID, DirectMessage> getLatestBulk(Collection<UUID> conversationIds) {
    if (conversationIds.isEmpty()) {
      return Map.of();
    }
    return loadDirectMessagePort.findLatestByConversationIds(conversationIds);
  }

  @Override
  public void read(UUID conversationId, UUID directMessageId, UUID myId) {
      DirectMessage directMessage = loadDirectMessagePort.findById(directMessageId).orElseThrow(()->{
        log.error("directMessage not found");
        return new MoplException(ErrorCode.DIRECT_MESSAGE_NOT_FOUND);
      });
      // 요청 경로의 conversationId가 실제 이 메시지가 속한 대화와 일치하는지,
      // 그리고 호출자가 이 메시지의 수신자 본인인지 명시적으로 검증한다.
      // (이전엔 "발신자가 아니면 통과"만 확인해서, 대화 참여자가 아닌 제3자도
      // directMessageId만 알면 남의 메시지를 읽음 처리할 수 있었다 - IDOR)
      if (!directMessage.getConversationId().equals(conversationId)) {
        log.warn("directMessage {} does not belong to conversation {}", directMessageId, conversationId);
        throw new MoplException(ErrorCode.FORBIDDEN_ACCESS);
      }
      if (!directMessage.getReceiverId().equals(myId)) {
        log.warn("user {} is not the receiver of directMessage {}", myId, directMessageId);
        throw new MoplException(ErrorCode.FORBIDDEN_ACCESS);
      }
      directMessage.markAsRead();
      saveDirectMessagePort.save(directMessage);
  }

  @Override
  public DirectMessage send(UUID conversationId, String content, UUID senderId, UUID receiverId) {
    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, content);
    return saveDirectMessagePort.save(directMessage);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<DirectMessageDto> getList(UUID conversationId,
      DirectMessageSearchCondition condition) {
    CursorPageResponse<DirectMessage> result = loadDirectMessagePort.findList(conversationId, condition);

    // 페이지 안 DM들의 발신/수신자 ID를 모아서 한 번에 조회 (N+1 방지)
    Set<UUID> userIds = new HashSet<>();
    result.data().forEach(dm -> {
      userIds.add(dm.getSenderId());
      userIds.add(dm.getReceiverId());
    });
    Map<UUID, UserSummary> users = userIds.isEmpty()
        ? Map.of()
        : loadUserPort.getUserSummaries(userIds);

    return new CursorPageResponse<>(
        result.data().stream().map(dm -> new DirectMessageDto(
            dm.getId(),
            dm.getConversationId(),
            dm.getCreatedAt(),
            users.get(dm.getSenderId()),
            users.get(dm.getReceiverId()),
            dm.getContent()
        )).toList(),
        result.nextCursor(),
        result.nextIdAfter(),
        result.hasNext(),
        result.totalCount(),
        result.sortBy(),
        result.sortDirection()
    );
  }

  @Override
  public List<UUID> findConversationIdsByContent(String keyword) {
    return loadDirectMessagePort.findConversationIdsByContent(keyword);
  }
}
