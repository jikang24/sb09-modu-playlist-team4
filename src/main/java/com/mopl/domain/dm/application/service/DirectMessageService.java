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
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import java.util.List;
import java.util.Optional;
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
  public Optional<DirectMessage> getLatest(UUID conversationId) {
    return loadDirectMessagePort.findLatestByConversationId(conversationId);
  }

  @Override
  public void read(UUID conversationId, UUID directMessageId, UUID myId) {
      DirectMessage directMessage = loadDirectMessagePort.findById(directMessageId).orElseThrow(()->{
        log.error("directMessage not found");
        return new MoplException(ErrorCode.DIRECT_MESSAGE_NOT_FOUND);
      });
      if(directMessage.isSender(myId)){
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
    CursorPageResponse<DirectMessage> result =loadDirectMessagePort.findList(conversationId, condition);
    return new CursorPageResponse<>(
        result.data().stream().map(dm -> new DirectMessageDto(
            dm.getId(),
            dm.getConversationId(),
            dm.getCreatedAt(),
            loadUserPort.getUserSummary(dm.getSenderId()),
            loadUserPort.getUserSummary(dm.getReceiverId()),
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
