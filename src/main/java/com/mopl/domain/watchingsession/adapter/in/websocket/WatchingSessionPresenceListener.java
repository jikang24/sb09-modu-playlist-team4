package com.mopl.domain.watchingsession.adapter.in.websocket;

import com.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.mopl.domain.watchingsession.service.WatchingSessionService;
import com.mopl.global.jwt.JwtClaims;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * 프론트가 "같이보기 입장/퇴장" REST(POST/DELETE .../watching-sessions)를 호출하지 않고
 * /sub/contents/{contentId}/watch 토픽을 구독/구독해제하는 것만으로 시청 상태를 표현하고 있어서,
 * 그 STOMP 생명주기(SUBSCRIBE/UNSUBSCRIBE/DISCONNECT)에 맞춰 서버가 직접 입장/퇴장을 처리한다.
 * (REST 엔드포인트는 그대로 유지 - 다른 클라이언트가 명시적으로 호출할 수도 있음)
 *
 * 같은 유저가 탭을 전환하면(콘텐츠X 구독 중 콘텐츠Y를 새로 구독) enter()가 내부에서 X 세션을 정리하고
 * Y 세션으로 교체한다. 이후 탭X 쪽 UNSUBSCRIBE/DISCONNECT가 뒤늦게 도착해도, 그 시점엔 이미 Y가 활성
 * 세션이므로 "내가 만든 세션이 지금도 활성 세션일 때만" 지우도록 watchingSessionId로 확인한다
 * (WatchingSessionService.leaveIfCurrent). 그냥 watcherId만으로 지우면 탭Y가 엉뚱하게 퇴장 처리된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionPresenceListener {

  private static final Pattern WATCH_DESTINATION = Pattern.compile("^/sub/contents/([^/]+)/watch$");

  private final WatchingSessionService watchingSessionService;

  // 세션(WebSocket 연결)당 현재 활성 watch 구독 하나만 추적 - 프론트가 콘텐츠 하나만 동시에 보는 구조를 그대로 반영
  private final Map<String, ActiveWatch> activeWatches = new ConcurrentHashMap<>();

  private record ActiveWatch(String subscriptionId, UUID watchingSessionId) {}

  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    UUID contentId = extractContentId(accessor.getDestination());
    if (contentId == null) {
      return;
    }

    UUID watcherId = extractWatcherId(accessor);
    if (watcherId == null) {
      log.warn("[WatchingSession] 구독 기반 입장 실패 - 인증 정보 없음, contentId: {}", contentId);
      return;
    }

    try {
      WatchingSessionDto dto = watchingSessionService.enter(watcherId, contentId);
      activeWatches.put(accessor.getSessionId(), new ActiveWatch(accessor.getSubscriptionId(), dto.id()));
      log.info("[WatchingSession] 구독 기반 입장 - contentId: {}, watcherId: {}", contentId, watcherId);
    } catch (Exception e) {
      log.warn("[WatchingSession] 구독 기반 입장 실패 - contentId: {}, 원인: {}", contentId, e.getMessage());
    }
  }

  @EventListener
  public void handleUnsubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId();
    String subscriptionId = accessor.getSubscriptionId();

    ActiveWatch active = activeWatches.get(sessionId);
    if (active == null || !active.subscriptionId().equals(subscriptionId)) {
      return; // 이 세션이 추적 중인 watch 구독 해제가 아니면(예: chat 구독 해제) 무시
    }

    activeWatches.remove(sessionId);
    leaveIfCurrent(accessor, sessionId, active.watchingSessionId());
  }

  @EventListener
  public void handleDisconnect(SessionDisconnectEvent event) {
    String sessionId = event.getSessionId();
    ActiveWatch active = activeWatches.remove(sessionId);
    if (active == null) {
      return; // watch 구독 중이 아니었으면(채팅만 하고 있었거나 등) 처리할 것 없음
    }
    leaveIfCurrent(StompHeaderAccessor.wrap(event.getMessage()), sessionId, active.watchingSessionId());
  }

  private void leaveIfCurrent(StompHeaderAccessor accessor, String sessionId, UUID watchingSessionId) {
    UUID watcherId = extractWatcherId(accessor);
    if (watcherId == null) {
      log.warn("[WatchingSession] 구독 기반 퇴장 실패 - sessionId: {}에서 인증 정보를 찾을 수 없음", sessionId);
      return;
    }
    try {
      // watcherId의 활성 세션이 지금도 watchingSessionId와 같을 때만 실제로 퇴장 처리됨
      // (탭 전환으로 이미 다른 세션이 활성화됐다면 조용히 무시 - 그 세션은 자기 자신의 이벤트로 정리됨)
      watchingSessionService.leaveIfCurrent(watcherId, watchingSessionId);
      log.info("[WatchingSession] 구독 기반 퇴장 - watcherId: {}, watchingSessionId: {}", watcherId, watchingSessionId);
    } catch (Exception e) {
      log.warn("[WatchingSession] 구독 기반 퇴장 실패 - watcherId: {}, 원인: {}", watcherId, e.getMessage());
    }
  }

  private UUID extractContentId(String destination) {
    if (destination == null) {
      return null;
    }
    Matcher matcher = WATCH_DESTINATION.matcher(destination);
    if (!matcher.matches()) {
      return null;
    }
    try {
      return UUID.fromString(matcher.group(1));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private UUID extractWatcherId(StompHeaderAccessor accessor) {
    if (accessor.getSessionAttributes() == null) {
      return null;
    }
    Object claims = accessor.getSessionAttributes().get("claims");
    if (!(claims instanceof JwtClaims jwtClaims)) {
      return null;
    }
    return jwtClaims.getUserId();
  }
}