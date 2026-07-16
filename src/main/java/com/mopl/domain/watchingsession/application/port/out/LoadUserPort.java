package com.mopl.domain.watchingsession.application.port.out;

import com.mopl.global.dto.UserSummary;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface LoadUserPort {
  UserSummary getUserSummary(UUID userId);

  /** 목록 조회에서 세션마다 유저를 개별 조회하지 않도록 배치로 가져옴 (N+1 방지용) */
  Map<UUID, UserSummary> getUserSummaries(Collection<UUID> userIds);
}