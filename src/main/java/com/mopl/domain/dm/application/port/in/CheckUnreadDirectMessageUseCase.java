package com.mopl.domain.dm.application.port.in;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface CheckUnreadDirectMessageUseCase {
  boolean hasUnread(UUID conversationId, UUID myId);

  /** 주어진 대화방들 중 안 읽은 메시지가 있는 대화방 ID만 한 번에 조회 (N+1 방지) */
  Set<UUID> hasUnreadBulk(Collection<UUID> conversationIds, UUID myId);

}
