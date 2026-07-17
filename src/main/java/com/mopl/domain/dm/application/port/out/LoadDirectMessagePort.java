package com.mopl.domain.dm.application.port.out;

import com.mopl.domain.dm.application.dto.DirectMessageSearchCondition;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.response.CursorPageResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LoadDirectMessagePort {
  Optional<DirectMessage> findById(UUID directMessageId);
  Optional<DirectMessage> findLatestByConversationId(UUID conversationId);
  boolean hasUnRead(UUID conversationId, UUID myId);
  CursorPageResponse<DirectMessage> findList(
      UUID conversationId, DirectMessageSearchCondition condition
  );
  List<UUID> findConversationIdsByContent(String keyword);

  /** 여러 대화방의 최근 메시지를 한 번에 조회 (N+1 방지) */
  Map<UUID, DirectMessage> findLatestByConversationIds(Collection<UUID> conversationIds);

  /** 주어진 대화방들 중 myId가 안 읽은 메시지가 있는 대화방 ID만 골라서 한 번에 조회 (N+1 방지) */
  Set<UUID> findConversationIdsWithUnread(Collection<UUID> conversationIds, UUID myId);

}
