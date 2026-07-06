package com.mopl.domain.conversation.application.port.out;

import com.mopl.global.dto.UserSummary;
import java.util.List;
import java.util.UUID;

public interface LoadUserPort {
  UserSummary getUserSummary(UUID userId);
  List<UUID> findUserIdsByNameLike(String keyword);

}
