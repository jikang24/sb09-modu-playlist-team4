package com.mopl.domain.conversation.application.port.out;

import com.mopl.global.dto.UserSummary;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LoadUserPort {
  UserSummary getUserSummary(UUID userId);
  Map<UUID, UserSummary> getUserSummaries(Collection<UUID> userIds);
  List<UUID> findUserIdsByNameLike(String keyword);

}
