package com.mopl.domain.dm.application.port.in;

import java.util.UUID;

public interface ReadDirectMessageUseCase {
  void read(UUID conversationId, UUID directMessageId, UUID myId);
}
