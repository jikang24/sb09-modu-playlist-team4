package com.mopl.domain.content.adapter.port;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface LoadWatcherCountPort {

  /** 특정 콘텐츠를 지금 보고 있는 시청자 수 */
  long countByContentId(UUID contentId);

  /** 목록 조회에서 콘텐츠마다 개별 조회하지 않도록 배치로 가져옴 (N+1 방지용) */
  Map<UUID, Long> countByContentIds(Collection<UUID> contentIds);
}