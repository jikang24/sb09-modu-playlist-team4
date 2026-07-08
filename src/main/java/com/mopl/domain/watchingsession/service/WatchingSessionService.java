package com.mopl.domain.watchingsession.service;

import com.mopl.domain.watchingsession.adapter.port.LoadContentPort;
import com.mopl.domain.watchingsession.adapter.port.LoadUserPort;
import com.mopl.domain.watchingsession.domain.WatchingSession;
import com.mopl.domain.watchingsession.dto.WatchingSessionChange;
import com.mopl.domain.watchingsession.dto.WatchingSessionChange.ChangeType;
import com.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.mopl.domain.watchingsession.dto.WatchingSessionSearchRequest;
import com.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import com.mopl.global.dto.ContentSummary;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.event.WatchingSessionStartedEvent;
import com.mopl.global.response.CursorPageResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private final WatchingSessionRepository watchingSessionRepository;
  private final LoadUserPort loadUserPort;
  private final LoadContentPort loadContentPort;
  private final SimpMessagingTemplate messagingTemplate;
  private final ApplicationEventPublisher applicationEventPublisher;

  /** 시청 입장 - 이미 다른 콘텐츠를 보고 있었다면 그 세션은 자동 종료됨 */
  public WatchingSessionDto enter(UUID watcherId, UUID contentId) {
    WatchingSession session = watchingSessionRepository.enter(watcherId, contentId);
    WatchingSessionDto dto = toDto(session);
    broadcast(ChangeType.JOIN, contentId, dto);
    applicationEventPublisher.publishEvent(
        new WatchingSessionStartedEvent(watcherId, contentId, dto.content().title()));
    return dto;
  }

  /** 시청 퇴장 - 보고 있는 게 없으면 WATCHING_SESSION_NOT_FOUND */
  public void leave(UUID watcherId) {
    WatchingSession session = watchingSessionRepository.leave(watcherId);
    WatchingSessionDto dto = toDto(session);
    broadcast(ChangeType.LEAVE, session.contentId(), dto);
  }

  /**
   * STOMP 구독 해제/연결 종료처럼 자동으로 걸리는 퇴장 - sessionId로 지금 활성 세션이 맞는지 확인 후 처리.
   * 탭 전환 등으로 이미 다른 세션으로 교체됐으면 조용히 무시한다 (사용자가 직접 요청한 게 아니라 에러로 취급하지 않음).
   */
  public void leaveIfCurrent(UUID watcherId, UUID sessionId) {
    watchingSessionRepository.leaveIfCurrent(watcherId, sessionId)
        .ifPresent(session -> broadcast(ChangeType.LEAVE, session.contentId(), toDto(session)));
  }

  /** 시청자 입장/퇴장을 같은 콘텐츠를 보고 있는 다른 시청자들에게 실시간으로 알림 */
  private void broadcast(ChangeType type, UUID contentId, WatchingSessionDto dto) {
    long watcherCount = watchingSessionRepository.countByContentId(contentId);
    WatchingSessionChange change = new WatchingSessionChange(type, dto, watcherCount);
    messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/watch", change);
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

    // 페이지 안의 세션이 전부 같은 contentId라 한 번만 조회해서 재사용 (N+1 방지)
    ContentSummary content = loadContentPort.getContent(request.contentId());

    // watcherId별로 한 번에 배치 조회 (N+1 방지)
    List<UUID> watcherIds = pageData.stream().map(WatchingSession::watcherId).distinct().toList();
    Map<UUID, UserSummary> watchersById = loadUserPort.getUserSummaries(watcherIds);

    List<WatchingSessionDto> data = pageData.stream()
        .map(session -> new WatchingSessionDto(
            session.id(), session.createdAt(), watchersById.get(session.watcherId()), content))
        .toList();

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