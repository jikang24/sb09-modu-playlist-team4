package com.mopl.domain.dm.application.port.out;

import com.mopl.global.dto.UserSummary;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface LoadUserPort {
  UserSummary getUserSummary(UUID userId);

  Map<UUID, UserSummary> getUserSummaries(Collection<UUID> userIds);
}
