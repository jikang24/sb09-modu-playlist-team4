package com.mopl.domain.watchingsession.repository;

import com.mopl.domain.watchingsession.domain.WatchingSession;
import com.mopl.domain.watchingsession.dto.WatchingSessionSearchRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchingSessionRepository {

  /** 시청 입장 - 기존에 보고 있던 세션이 있으면 자동으로 정리하고 새로 시작 */
  WatchingSession enter(UUID watcherId, UUID contentId);

  /** 시청 퇴장 - 현재 보고 있는 세션이 없으면 예외. 브로드캐스트용으로 종료된 세션을 반환 */
  WatchingSession leave(UUID watcherId);

  Optional<WatchingSession> findByWatcherId(UUID watcherId);

  List<WatchingSession> findByContentId(WatchingSessionSearchRequest request);

  long countByContentId(UUID contentId);
}