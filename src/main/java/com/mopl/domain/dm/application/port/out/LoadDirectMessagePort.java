package com.mopl.domain.dm.application.port.out;

import com.mopl.domain.dm.application.dto.DirectMessageSearchCondition;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.response.CursorPageResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadDirectMessagePort {
  Optional<DirectMessage> findById(UUID directMessageId);
  Optional<DirectMessage> findLatestByConversationId(UUID conversationId);
  boolean hasUnRead(UUID conversationId, UUID myId);
  CursorPageResponse<DirectMessage> findList(
      UUID conversationId, DirectMessageSearchCondition condition
  );
  List<UUID> findConversationIdsByContent(String keyword);

}
