package com.mopl.domain.dm.application.port.in;

import java.util.UUID;

public interface CheckUnreadDirectMessageUseCase {
  boolean hasUnLread(UUID conversationId, UUID myId);

}
