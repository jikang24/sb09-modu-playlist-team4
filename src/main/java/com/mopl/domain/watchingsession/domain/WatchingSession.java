package com.mopl.domain.watchingsession.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 시청 세션 (실시간 "같이 보기" 참여 상태)
 *
 * Redis로만 관리 (실시간 입/퇴장이 잦아 DB 부하가 크기 때문)
 * 한 사용자는 동시에 하나의 콘텐츠만 시청 중일 수 있음 (새로 입장하면 기존 세션은 자동 종료)
 */
public record WatchingSession(
    UUID id,
    UUID watcherId,
    UUID contentId,
    Instant createdAt
) {

  public static WatchingSession create(UUID watcherId, UUID contentId) {
    return new WatchingSession(UUID.randomUUID(), watcherId, contentId, Instant.now());
  }
}