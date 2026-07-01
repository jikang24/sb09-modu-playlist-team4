package com.mopl.domain.dm.application.port.in;

import java.util.UUID;

public interface CheckUnreadDirectMessageUseCase {
  boolean hasUnread(UUID conversationId, UUID myId);

}
