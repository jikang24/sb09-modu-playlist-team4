package com.mopl.domain.dm.application.port.in;

import java.util.List;
import java.util.UUID;

public interface GetConversationIdsByContentUseCase {
  List<UUID> findConversationIdsByContent(String keyword);

}
