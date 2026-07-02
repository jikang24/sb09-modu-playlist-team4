package com.mopl.domain.conversation.application.port.in;

import com.mopl.domain.conversation.application.dto.ConversationSearchCondition;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.global.response.CursorPageResponse;
import java.util.UUID;

public interface GetConversationListUseCase {
  CursorPageResponse<Conversation> getList(UUID myId, ConversationSearchCondition conversationSearchCondition);

}
