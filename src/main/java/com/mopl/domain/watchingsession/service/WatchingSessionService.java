package com.mopl.domain.watchingsession.service;

import com.mopl.domain.watchingsession.adapter.port.LoadContentPort;
import com.mopl.domain.watchingsession.adapter.port.LoadUserPort;
import com.mopl.domain.watchingsession.domain.WatchingSession;
import com.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.mopl.domain.watchingsession.dto.WatchingSessionSearchRequest;
import com.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import com.mopl.global.dto.ContentSummary;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.response.CursorPageResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private final WatchingSessionRepository watchingSessionRepository;
  private final LoadUserPort loadUserPort;
  private final LoadContentPort loadContentPort;

  /** 시청 입장 - 이미 다른 콘텐츠를 보고 있었다면 그 세션은 자동 종료됨 */
  public WatchingSessionDto enter(UUID watcherId, UUID contentId) {
    WatchingSession session = watchingSessionRepository.enter(watcherId, contentId);
    return toDto(session);
  }

  /** 시청 퇴장 - 보고 있는 게 없으면 WATCHING_SESSION_NOT_FOUND */
  public void leave(UUID watcherId) {
    watchingSessionRepository.leave(watcherId);
  }

  /** 특정 사용자가 지금 보고 있는 세션 (없으면 null) */
  public WatchingSessionDto getByWatcherId(UUID watcherId) {
    return watchingSessionRepository.findByWatcherId(watcherId)
        .map(this::toDto)
        .orElse(null);
  }

  public CursorPageResponse<WatchingSessionDto> getByContentId(WatchingSessionSearchRequest request) {
    List<WatchingSession> sessions = watchingSessionRepository.findByContentId(request);

    boolean hasNext = sessions.size() > request.limit();
    List<WatchingSession> pageData = hasNext ? sessions.subList(0, request.limit()) : sessions;

    List<WatchingSessionDto> data = pageData.stream().map(this::toDto).toList();

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !pageData.isEmpty()) {
      WatchingSession last = pageData.get(pageData.size() - 1);
      nextCursor = last.createdAt().toString();
      nextIdAfter = last.id();
    }

    long totalCount = watchingSessionRepository.countByContentId(request.contentId());

    return new CursorPageResponse<>(
        data, nextCursor, nextIdAfter, hasNext, totalCount,
        request.sortBy(), request.sortDirection());
  }

  private WatchingSessionDto toDto(WatchingSession session) {
    UserSummary watcher = loadUserPort.getUserSummary(session.watcherId());
    ContentSummary content = loadContentPort.getContent(session.contentId());
    return new WatchingSessionDto(session.id(), session.createdAt(), watcher, content);
  }
}