package com.mopl.domain.dm.application.port.in;

import com.mopl.domain.dm.domain.DirectMessage;
import java.util.UUID;

public interface SendDirectMessageUseCase {
  DirectMessage send(UUID conversationId, String content, UUID senderId,UUID receiverId);

}
