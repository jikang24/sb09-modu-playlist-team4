package com.mopl.domain.watchingsession.service;

import com.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 콘텐츠 실시간 시청자 수 조회 전용 - WatchingSessionRepository에만 의존한다.
 *
 * content 등 다른 도메인이 watchingsession 리포지토리를 직접 참조하지 않도록 이 서비스를 통해서만
 * 접근하게 하되, WatchingSessionService와는 분리했다. WatchingSessionService는 LoadContentPort(→
 * content 도메인 역참조)를 갖고 있어서, content 쪽 어댑터가 WatchingSessionService를 직접 의존하면
 * content → watchingSessionService → watchingSessionContentAdapter → content로 순환 참조가 생긴다.
 */
@Service
@RequiredArgsConstructor
public class WatchingSessionCountService {

  private final WatchingSessionRepository watchingSessionRepository;

  public long countByContentId(UUID contentId) {
    return watchingSessionRepository.countByContentId(contentId);
  }

  public Map<UUID, Long> countByContentIds(Collection<UUID> contentIds) {
    return contentIds.stream()
        .distinct()
        .collect(Collectors.toMap(id -> id, watchingSessionRepository::countByContentId));
  }
}