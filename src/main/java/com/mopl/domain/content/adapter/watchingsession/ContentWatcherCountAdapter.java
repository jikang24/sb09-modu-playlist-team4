package com.mopl.domain.content.adapter.watchingsession;

import com.mopl.domain.content.adapter.port.LoadWatcherCountPort;
import com.mopl.domain.watchingsession.service.WatchingSessionService;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentWatcherCountAdapter implements LoadWatcherCountPort {

  // 다른 도메인(watchingsession)의 리포지토리를 직접 참조하지 않고 Service를 통해서만 접근
  private final WatchingSessionService watchingSessionService;

  @Override
  public long countByContentId(UUID contentId) {
    return watchingSessionService.countByContentId(contentId);
  }

  @Override
  public Map<UUID, Long> countByContentIds(Collection<UUID> contentIds) {
    // Redis ZCARD는 건당 비용이 매우 작아 배치 전용 명령 없이 단순 반복 조회로 처리
    return watchingSessionService.countByContentIds(contentIds);
  }
}