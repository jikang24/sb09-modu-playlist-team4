package com.mopl.domain.watchingsession.dto;

/** /sub/contents/{contentId}/watch 로 브로드캐스트되는 시청 세션 변경 이벤트 */
public record WatchingSessionChange(
    ChangeType type,
    WatchingSessionDto watchingSession,
    long watcherCount
) {
  public enum ChangeType { JOIN, LEAVE }
}