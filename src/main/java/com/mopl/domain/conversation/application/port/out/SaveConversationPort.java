package com.mopl.domain.conversation.application.port.out;

import com.mopl.domain.conversation.domain.Conversation;

public interface SaveConversationPort {
  Conversation save(Conversation conversation);

}
