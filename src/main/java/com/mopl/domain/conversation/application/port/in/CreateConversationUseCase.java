package com.mopl.domain.conversation.application.port.in;

import com.mopl.domain.conversation.domain.Conversation;
import java.util.UUID;

public interface CreateConversationUseCase {
  Conversation create(UUID userId1, UUID userId2);

}
