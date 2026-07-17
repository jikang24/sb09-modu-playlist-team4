package com.mopl.domain.watchingsession.application.port.in;

import com.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.mopl.domain.watchingsession.dto.WatchingSessionSearchRequest;
import com.mopl.global.response.CursorPageResponse;
import java.util.UUID;

public interface WatchingSessionUseCase {

  /** 시청 입장 - 이미 다른 콘텐츠를 보고 있었다면 그 세션은 자동 종료됨 */
  WatchingSessionDto enter(UUID watcherId, UUID contentId);

  /** 시청 퇴장 - 보고 있는 게 없으면 WATCHING_SESSION_NOT_FOUND */
  void leave(UUID watcherId);

  /**
   * STOMP 구독 해제/연결 종료처럼 자동으로 걸리는 퇴장 - sessionId로 지금 활성 세션이 맞는지 확인 후 처리.
   * 탭 전환 등으로 이미 다른 세션으로 교체됐으면 조용히 무시한다 (사용자가 직접 요청한 게 아니라 에러로 취급하지 않음).
   */
  void leaveIfCurrent(UUID watcherId, UUID sessionId);

  /** 특정 사용자가 지금 보고 있는 세션 (없으면 null) */
  WatchingSessionDto getByWatcherId(UUID watcherId);

  CursorPageResponse<WatchingSessionDto> getByContentId(WatchingSessionSearchRequest request);
}