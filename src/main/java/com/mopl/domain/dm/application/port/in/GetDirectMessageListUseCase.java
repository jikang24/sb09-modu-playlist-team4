package com.mopl.domain.dm.application.port.in;

import com.mopl.domain.dm.application.dto.DirectMessageSearchCondition;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.response.CursorPageResponse;
import java.util.UUID;

public interface GetDirectMessageListUseCase {
  CursorPageResponse<DirectMessage> getList(UUID conversationId,  DirectMessageSearchCondition condition);

}
