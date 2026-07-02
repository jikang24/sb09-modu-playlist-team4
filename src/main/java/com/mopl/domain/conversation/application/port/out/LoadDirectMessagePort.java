package com.mopl.domain.conversation.application.port.out;

import java.util.List;
import java.util.UUID;

public interface LoadDirectMessagePort {
  List<UUID> findConversationIdsByContent(String keyword);

}
