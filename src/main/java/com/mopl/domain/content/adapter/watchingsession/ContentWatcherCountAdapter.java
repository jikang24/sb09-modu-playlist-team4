package com.mopl.domain.content.adapter.watchingsession;

import com.mopl.domain.content.adapter.port.LoadWatcherCountPort;
import com.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentWatcherCountAdapter implements LoadWatcherCountPort {

  private final WatchingSessionRepository watchingSessionRepository;

  @Override
  public long countByContentId(UUID contentId) {
    return watchingSessionRepository.countByContentId(contentId);
  }

  @Override
  public Map<UUID, Long> countByContentIds(Collection<UUID> contentIds) {
    // Redis ZCARD는 건당 비용이 매우 작아 배치 전용 명령 없이 단순 반복 조회로 처리
    return contentIds.stream()
        .distinct()
        .collect(Collectors.toMap(id -> id, watchingSessionRepository::countByContentId));
  }
}