package com.mopl.domain.dm.application.port.in;

import com.mopl.domain.dm.domain.DirectMessage;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface GetLatestDirectMessageUseCase {
  Optional<DirectMessage> getLatest(UUID conversationId);

  /** 여러 대화방의 최근 메시지를 한 번에 조회 (N+1 방지) */
  Map<UUID, DirectMessage> getLatestBulk(Collection<UUID> conversationIds);
}
