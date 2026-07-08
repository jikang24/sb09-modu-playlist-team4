package com.mopl.domain.watchingsession.adapter.in.websocket;

import com.mopl.domain.watchingsession.service.WatchingSessionService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionPresenceListener {

  private static final Pattern WATCH_DESTINATION = Pattern.compile("^/sub/contents/([^/]+)/watch$");

  private final WatchingSessionService watchingSessionService;

  // 세션(WebSocket 연결)당 현재 활성 watch 구독 하나만 추적 - 프론트가 콘텐츠 하나만 동시에 보는 구조를 그대로 반영
  private final Map<String, String> activeWatchSubscriptions = new ConcurrentHashMap<>();

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
      watchingSessionService.enter(watcherId, contentId);
      activeWatchSubscriptions.put(accessor.getSessionId(), accessor.getSubscriptionId());
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

    String activeSubscriptionId = activeWatchSubscriptions.get(sessionId);
    if (activeSubscriptionId == null || !activeSubscriptionId.equals(subscriptionId)) {
      return; // 이 세션이 추적 중인 watch 구독 해제가 아니면(예: chat 구독 해제) 무시
    }

    activeWatchSubscriptions.remove(sessionId);
    leave(accessor, sessionId);
  }

  @EventListener
  public void handleDisconnect(SessionDisconnectEvent event) {
    String sessionId = event.getSessionId();
    if (activeWatchSubscriptions.remove(sessionId) == null) {
      return; // watch 구독 중이 아니었으면(채팅만 하고 있었거나 등) 처리할 것 없음
    }
    leave(StompHeaderAccessor.wrap(event.getMessage()), sessionId);
  }

  private void leave(StompHeaderAccessor accessor, String sessionId) {
    UUID watcherId = extractWatcherId(accessor);
    if (watcherId == null) {
      log.warn("[WatchingSession] 구독 기반 퇴장 실패 - sessionId: {}에서 인증 정보를 찾을 수 없음", sessionId);
      return;
    }
    try {
      watchingSessionService.leave(watcherId);
      log.info("[WatchingSession] 구독 기반 퇴장 - watcherId: {}", watcherId);
    } catch (MoplException e) {
      if (e.getErrorCode() != ErrorCode.WATCHING_SESSION_NOT_FOUND) {
        log.warn("[WatchingSession] 구독 기반 퇴장 실패 - watcherId: {}, 원인: {}", watcherId, e.getMessage());
      }
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