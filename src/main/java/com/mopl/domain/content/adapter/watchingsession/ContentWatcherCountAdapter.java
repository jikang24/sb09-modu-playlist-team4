package com.mopl.domain.content.adapter.watchingsession;

import com.mopl.domain.content.adapter.port.LoadWatcherCountPort;
import com.mopl.domain.watchingsession.service.WatchingSessionCountService;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentWatcherCountAdapter implements LoadWatcherCountPort {

  // WatchingSessionRepository를 직접 참조하지 않고 Service를 통해서만 접근.
  // WatchingSessionService(전체)가 아니라 WatchingSessionCountService(카운트 전용)를 쓰는 이유:
  // WatchingSessionService는 LoadContentPort(→ content 도메인 역참조)를 갖고 있어서, 여기서
  // WatchingSessionService를 직접 의존하면 content → watchingSessionService →
  // watchingSessionContentAdapter → content로 순환 참조가 생긴다.
  private final WatchingSessionCountService watchingSessionCountService;

  @Override
  public long countByContentId(UUID contentId) {
    return watchingSessionCountService.countByContentId(contentId);
  }

  @Override
  public Map<UUID, Long> countByContentIds(Collection<UUID> contentIds) {
    // Redis ZCARD는 건당 비용이 매우 작아 배치 전용 명령 없이 단순 반복 조회로 처리
    return watchingSessionCountService.countByContentIds(contentIds);
  }
}