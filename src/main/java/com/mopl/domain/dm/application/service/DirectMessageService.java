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
      // 요청 경로의 conversationId가 실제 이 메시지가 속한 대화와 일치하는지 검증한다.
      // (이게 안 맞으면 대화 참여자가 아닌 제3자가 directMessageId만 알고 엉뚱한
      // conversationId로 접근하려는 경우일 수 있다 - IDOR)
      if (!directMessage.getConversationId().equals(conversationId)) {
        log.warn("directMessage {} does not belong to conversation {}", directMessageId, conversationId);
        throw new MoplException(ErrorCode.FORBIDDEN_ACCESS);
      }
      // 프론트는 웹소켓 구독(/sub/conversations/{id}/direct-messages)으로 들어오는 메시지마다
      // 무조건 읽음 처리를 호출한다. 이 구독은 발신자 본인 화면에도 실시간 반영을 위해
      // 걸려있어서, 내가 보낸 메시지에 대해서도 내 클라이언트가 이 API를 호출한다.
      // 발신자 본인의 호출은 위험한 접근이 아니라 의미 없는 호출일 뿐이므로 조용히 no-op 처리하고,
      // 대화 참여자도 아닌(수신자도 발신자도 아닌) 진짜 제3자만 막는다.
      if (directMessage.isSender(myId)) {
        return;
      }
      if (!directMessage.getReceiverId().equals(myId)) {
        log.warn("user {} is neither sender nor receiver of directMessage {}", myId, directMessageId);
        throw new MoplException(ErrorCode.FORBIDDEN_ACCESS);
      }
      // 프론트는 방을 열 때 "가장 최근 메시지" 하나에 대해서만 이 API를 호출한다.
      // 그 사이 쌓인, 아직 read=false인 이전 메시지들이 이 메시지 하나만 처리하고 나면
      // 영원히 안 읽은 채로 남아 목록 화면에서 다시 안읽음 표시가 뜨는 문제가 있었다.
      // 그래서 단건 갱신 대신, 이 메시지 시각까지 나(수신자)한테 온 안 읽은 메시지를
      // 전부 한 번에 읽음 처리한다.
      saveDirectMessagePort.markAllAsReadUpTo(conversationId, myId, directMessage.getCreatedAt());
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
