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

  /**
   * 지금 활성 세션이 sessionId와 같을 때만 퇴장 처리한다.
   * 이미 다른 세션으로 교체됐으면(예: 탭 전환) 아무 것도 하지 않고 빈 값을 반환한다.
   * STOMP 구독 해제/연결 종료처럼 "늦게 도착할 수 있는" 이벤트 기반 퇴장에 사용.
   */
  Optional<WatchingSession> leaveIfCurrent(UUID watcherId, UUID sessionId);

  Optional<WatchingSession> findByWatcherId(UUID watcherId);

  List<WatchingSession> findByContentId(WatchingSessionSearchRequest request);

  long countByContentId(UUID contentId);
}